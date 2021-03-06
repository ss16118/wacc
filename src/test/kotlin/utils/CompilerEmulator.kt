package utils

import CompilerMode
import ast.WaccAST
import codegen.AstToRawArmConverter
import codegen.RegisterAllocator
import exceptions.SemanticException
import exceptions.SyntacticException
import parser.Parser
import semantics.SemanticAnalyzer
import CompilerMode.*
import java.io.*
import java.lang.Exception
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class CompilerEmulator(private val inputFile: File,
                       private val mode: CompilerMode,
                       private val errorStream: PrintStream = PrintStream(NullOutputStream())) {

    private val compiler: String = "arm-linux-gnueabi-gcc"

    class EmulatorResult(val exitCode: Int, val ast: WaccAST?, val exception: Exception?, val output: String)

    fun run(inputData: List<String> = emptyList()): EmulatorResult {
        val parser = Parser(inputFile.inputStream(), errorStream = errorStream)
        var exitCode = 0
        var exception: Exception? = null
        val sa = SemanticAnalyzer()

        /* Perform parsing */
        val ast = try {
            val temp = parser.parseProgram()
            if (mode != PARSE_ONLY) {
                sa.suppressWarning().doCheck(temp)
            }
            println("Compilation completed: ${inputFile.path}")
            temp
        } catch (sye: SyntacticException) {
            System.err.println("PARSE ERROR: ${inputFile.path}")
            exception = sye
            exitCode = 100
            null
        } catch (sme: SemanticException) {
            System.err.println("SEM ERROR: ${inputFile.path}")
            exception = sme
            exitCode = 200 // 200 as semantic error exit code
            null
        } catch (e: Exception) {
            System.err.println("FATAL ERROR: ${inputFile.path}")
            System.err.println("A non-syntactic, non-semantic exception was thrown!")
            System.err.println(e.message)
            System.err.println(e.localizedMessage)
            System.err.println(e.cause)
            e.printStackTrace(System.err)
            exception = e
            exitCode = 1 //  error output
            null
        }

        /* If parse was successful and emulated execution is required */
        var programOutput: String = ""
        if (mode == EXECUTE && ast != null) {
            val raw = AstToRawArmConverter(ast, sa.symbolTable).translate().export()
            val assembly = RegisterAllocator(raw).run().toString()
            val temp = File("src/test/kotlin/utils/temp.s")
            if (!temp.exists()) {
                temp.createNewFile()
            }
            temp.bufferedWriter().use { it.write(assembly + "\n") }
            val executableFile: File? = compileAssembly(temp)
            if (executableFile != null) {
                val process = ProcessBuilder("qemu-arm", "-L", "/usr/arm-linux-gnueabi/",
                        executableFile.absolutePath).start()
                if (inputData.isNotEmpty()) {
                    val writer = BufferedWriter(OutputStreamWriter(process.outputStream, "UTF-8"))
                    for (line: String in inputData) {
                        writer.write(line)
                        writer.flush()
                    }
                    writer.close()
                }

                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroy()
                    throw TimeoutException("Program timed out!")
                }
                process.inputStream.reader(Charsets.UTF_8).use {
                    programOutput += it.readText()
                }

                val addressRegex = """0x\w{5}""".toRegex()
                programOutput = programOutput.replace(addressRegex, "0x*****")

                exitCode = process.exitValue()
            }
        }
        return EmulatorResult(exitCode, ast, exception, programOutput)
    }

    /**
     * Compiles the assembly file to executable file
     */
    private fun compileAssembly(assemblyFile: File): File? {
        var executableFile: File? = null
        try {
            val objectFilePath = assemblyFile.parent + "/temp"
            val process = ProcessBuilder(compiler, "-o", objectFilePath,
                    "-mcpu=arm1176jzf-s", "-mtune=arm1176jzf-s", assemblyFile.absolutePath).start()
            process.waitFor()
            if (process.exitValue() != 0) {
                println("[FATAL] Compilation failed!: Failed to create $objectFilePath")
            }
            executableFile = File(objectFilePath)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            return executableFile
        }
    }

}