.data

singleton_0:
    .word 2
    .ascii "%s\0"
singleton_1:
    .word 2
    .ascii "%d\0"
singleton_7:
    .word 4
    .ascii "%d\n\0"
singleton_8:
    .word 4
    .ascii "%s\n\0"
msg_0:
    .word 24
    .ascii "Using fixed-point real: \0"
msg_1:
    .word 3
    .ascii " / \0"
msg_2:
    .word 3
    .ascii " * \0"
msg_3:
    .word 3
    .ascii " = \0"
msg_4:
    .word 81
    .ascii "OverflowError: the result is too small/large to store in a 4-byte signed-integer.\0"
msg_5:
    .word 43
    .ascii "DivideByZeroError: divide or modulo by zero\0"
.text

.global main
main:
    PUSH {lr}
    SUB sp, sp, #16
    MOV r4, #10
    STR r4, [sp, #8]
    MOV r4, #3
    STR r4, [sp, #4]
    LDR r4, =msg_0
    MOV r1, r4
    ADD r1, r1, #4
    LDR r0, =singleton_0
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    LDR r4, [sp, #8]
    MOV r1, r4
    LDR r0, =singleton_1
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    LDR r4, =msg_1
    MOV r1, r4
    ADD r1, r1, #4
    LDR r0, =singleton_0
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    LDR r4, [sp, #4]
    MOV r1, r4
    LDR r0, =singleton_1
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    LDR r4, =msg_2
    MOV r1, r4
    ADD r1, r1, #4
    LDR r0, =singleton_0
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    LDR r4, [sp, #4]
    MOV r1, r4
    LDR r0, =singleton_1
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    LDR r4, =msg_3
    MOV r1, r4
    ADD r1, r1, #4
    LDR r0, =singleton_0
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    LDR r4, [sp, #8]
    STR r4, [sp, #-4]!
    BL intToFixedPoint_0
    ADD sp, sp, #4
    STR r0, [sp]
    LDR r4, [sp, #4]
    STR r4, [sp, #-4]!
    LDR r4, [sp, #4]
    STR r4, [sp, #-4]!
    BL divideByInt_0
    ADD sp, sp, #8
    STR r0, [sp]
    LDR r4, [sp, #4]
    STR r4, [sp, #-4]!
    LDR r4, [sp, #4]
    STR r4, [sp, #-4]!
    BL multiplyByInt_0
    ADD sp, sp, #8
    STR r0, [sp]
    LDR r4, [sp]
    STR r4, [sp, #-4]!
    BL fixedPointToIntRoundNear_0
    ADD sp, sp, #4
    STR r0, [sp, #12]
    LDR r4, [sp, #12]
    MOV r1, r4
    LDR r0, =singleton_7
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    ADD sp, sp, #16
    MOV r0, #0
    POP {pc}
    .ltorg
q_0:
    PUSH {lr}
    MOV r0, #14
    POP {pc}
    .ltorg
power_0:
    PUSH {lr}
    SUB sp, sp, #4
    MOV r4, #1
    STR r4, [sp]
loop_check_0:
    LDR r4, [sp, #12]
    CMP r4, #0
    MOVGT r4, #1
    MOVLE r4, #0
    CMP r4, #0
    BEQ loop_end_0
loop_body_0:
    LDR r4, [sp]
    LDR r5, [sp, #8]
    SMULL r4, r6, r4, r5
    CMP r6, r4, ASR #31
    BLNE p_overflow_error
    STR r4, [sp]
    LDR r4, [sp, #12]
    SUBS r4, r4, #1
    BLVS p_overflow_error
    STR r4, [sp, #12]
    B loop_check_0
loop_end_0:
    LDR r4, [sp]
    ADD sp, sp, #4
    MOV r0, r4
    POP {pc}
    .ltorg
f_0:
    PUSH {lr}
    SUB sp, sp, #8
    BL q_0
    STR r0, [sp, #4]
    LDR r4, [sp, #4]
    STR r4, [sp, #-4]!
    MOV r4, #2
    STR r4, [sp, #-4]!
    BL power_0
    ADD sp, sp, #8
    STR r0, [sp]
    LDR r4, [sp]
    ADD sp, sp, #8
    MOV r0, r4
    POP {pc}
    .ltorg
intToFixedPoint_0:
    PUSH {lr}
    SUB sp, sp, #4
    BL f_0
    STR r0, [sp]
    LDR r4, [sp, #8]
    LDR r5, [sp]
    SMULL r4, r6, r4, r5
    CMP r6, r4, ASR #31
    BLNE p_overflow_error
    ADD sp, sp, #4
    MOV r0, r4
    POP {pc}
    .ltorg
fixedPointToIntRoundDown_0:
    PUSH {lr}
    SUB sp, sp, #4
    BL f_0
    STR r0, [sp]
    LDR r4, [sp, #8]
    LDR r5, [sp]
    MOV r0, r4
    MOV r1, r5
    BL p_check_div_by_zero
    BL __aeabi_idiv
    MOV r4, r0
    ADD sp, sp, #4
    MOV r0, r4
    POP {pc}
    .ltorg
fixedPointToIntRoundNear_0:
    PUSH {lr}
    SUB sp, sp, #4
    BL f_0
    STR r0, [sp]
    LDR r4, [sp, #8]
    CMP r4, #0
    MOVGE r4, #1
    MOVLT r4, #0
    CMP r4, #0
    BEQ if_else_0
if_then_0:
    LDR r4, [sp, #8]
    LDR r5, [sp]
    MOV r0, r5
    MOV r1, #2
    BL p_check_div_by_zero
    BL __aeabi_idiv
    MOV r5, r0
    ADDS r4, r4, r5
    BLVS p_overflow_error
    LDR r6, [sp]
    MOV r0, r4
    MOV r1, r6
    BL p_check_div_by_zero
    BL __aeabi_idiv
    MOV r4, r0
    ADD sp, sp, #4
    MOV r0, r4
    POP {pc}
if_then_0_seq_0:
    B if_end_0
if_else_0:
    LDR r7, [sp, #8]
    LDR r8, [sp]
    MOV r0, r8
    MOV r1, #2
    BL p_check_div_by_zero
    BL __aeabi_idiv
    MOV r8, r0
    SUBS r7, r7, r8
    BLVS p_overflow_error
    LDR r9, [sp]
    MOV r0, r7
    MOV r1, r9
    BL p_check_div_by_zero
    BL __aeabi_idiv
    MOV r7, r0
    ADD sp, sp, #4
    MOV r0, r7
    POP {pc}
if_else_0_seq_0:
    
if_end_0:
    .ltorg
add_0:
    PUSH {lr}
    LDR r4, [sp, #4]
    LDR r5, [sp, #8]
    ADDS r4, r4, r5
    BLVS p_overflow_error
    MOV r0, r4
    POP {pc}
    .ltorg
subtract_0:
    PUSH {lr}
    LDR r4, [sp, #4]
    LDR r5, [sp, #8]
    SUBS r4, r4, r5
    BLVS p_overflow_error
    MOV r0, r4
    POP {pc}
    .ltorg
addByInt_0:
    PUSH {lr}
    SUB sp, sp, #4
    BL f_0
    STR r0, [sp]
    LDR r4, [sp, #8]
    LDR r5, [sp, #12]
    LDR r6, [sp]
    SMULL r5, r7, r5, r6
    CMP r7, r5, ASR #31
    BLNE p_overflow_error
    ADDS r4, r4, r5
    BLVS p_overflow_error
    ADD sp, sp, #4
    MOV r0, r4
    POP {pc}
    .ltorg
subtractByInt_0:
    PUSH {lr}
    SUB sp, sp, #4
    BL f_0
    STR r0, [sp]
    LDR r4, [sp, #8]
    LDR r5, [sp, #12]
    LDR r6, [sp]
    SMULL r5, r7, r5, r6
    CMP r7, r5, ASR #31
    BLNE p_overflow_error
    SUBS r4, r4, r5
    BLVS p_overflow_error
    ADD sp, sp, #4
    MOV r0, r4
    POP {pc}
    .ltorg
multiply_0:
    PUSH {lr}
    SUB sp, sp, #4
    BL f_0
    STR r0, [sp]
    LDR r4, [sp, #8]
    LDR r5, [sp, #12]
    SMULL r4, r6, r4, r5
    CMP r6, r4, ASR #31
    BLNE p_overflow_error
    LDR r6, [sp]
    MOV r0, r4
    MOV r1, r6
    BL p_check_div_by_zero
    BL __aeabi_idiv
    MOV r4, r0
    ADD sp, sp, #4
    MOV r0, r4
    POP {pc}
    .ltorg
multiplyByInt_0:
    PUSH {lr}
    LDR r4, [sp, #4]
    LDR r5, [sp, #8]
    SMULL r4, r6, r4, r5
    CMP r6, r4, ASR #31
    BLNE p_overflow_error
    MOV r0, r4
    POP {pc}
    .ltorg
divide_0:
    PUSH {lr}
    SUB sp, sp, #4
    BL f_0
    STR r0, [sp]
    LDR r4, [sp, #8]
    LDR r5, [sp]
    SMULL r4, r6, r4, r5
    CMP r6, r4, ASR #31
    BLNE p_overflow_error
    LDR r6, [sp, #12]
    MOV r0, r4
    MOV r1, r6
    BL p_check_div_by_zero
    BL __aeabi_idiv
    MOV r4, r0
    ADD sp, sp, #4
    MOV r0, r4
    POP {pc}
    .ltorg
divideByInt_0:
    PUSH {lr}
    LDR r4, [sp, #4]
    LDR r5, [sp, #8]
    MOV r0, r4
    MOV r1, r5
    BL p_check_div_by_zero
    BL __aeabi_idiv
    MOV r4, r0
    MOV r0, r4
    POP {pc}
    .ltorg
p_runtime_error:
    MOV r0, #-1
    BL exit
p_overflow_error:
    LDR r4, =msg_4
    MOV r1, r4
    ADD r1, r1, #4
    LDR r0, =singleton_8
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    BL p_runtime_error
p_check_div_by_zero:
    PUSH {lr}
    CMP r1, #0
    BNE no_err_0
p_check_div_by_zero_seq_0:
    LDR r4, =msg_5
    MOV r1, r4
    ADD r1, r1, #4
    LDR r0, =singleton_8
    ADD r0, r0, #4
    BL printf
    MOV r0, #0
    BL fflush
    BLEQ p_runtime_error
no_err_0:
    POP {pc}