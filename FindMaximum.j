.class public FindMaximum
.super java/lang/Object

.field public _test_arr [I

.method public <init>()V
    aload_0
    invokenonvirtual java/lang/Object/<init>()V
    return
.end method

.method public find_maximum([I)I
    .limit stack 4
    .limit locals 5

    iconst_1
    istore_2

    aload_1
    iconst_0
    iaload
    istore_3

    iload_2
    aload_1
    arraylength
    if_icmplt lt_true_0
    iconst_0
    goto lt_false_0
  lt_true_0:
    iconst_1
  lt_false_0:
    ifeq end_while_0
  start_while_0:
    aload_1
    iload_2
    iaload
    istore 4
    iload_3
    iload 4
    if_icmplt lt_true_1
    iconst_0
    goto lt_false_1
  lt_true_1:
    iconst_1
  lt_false_1:
    ifeq else_0
    iload 4
    istore_3
    goto endif_0
  else_0:
  endif_0:
    iload_2
    iconst_1
    iadd
    istore_2
    iload_2
    aload_1
    arraylength
    if_icmplt lt_true_2
    iconst_0
    goto lt_false_2
  lt_true_2:
    iconst_1
  lt_false_2:
    ifne start_while_0
  end_while_0:


    iload_3
    ireturn

.end method

.method public build_test_arr()I
    .limit stack 4
    .limit locals 1

    aload_0
    iconst_5
    newarray int
    putfield FindMaximum/_test_arr [I

    aload_0
    getfield FindMaximum/_test_arr [I
    iconst_0
    bipush 14
    iastore

    aload_0
    getfield FindMaximum/_test_arr [I
    iconst_1
    bipush 28
    iastore

    aload_0
    getfield FindMaximum/_test_arr [I
    iconst_2
    iconst_0
    iastore

    aload_0
    getfield FindMaximum/_test_arr [I
    iconst_3
    iconst_0
    iconst_5
    isub
    iastore

    aload_0
    getfield FindMaximum/_test_arr [I
    iconst_4
    bipush 12
    iastore


    iconst_0
    ireturn

.end method

.method public get_array()[I
    .limit stack 1
    .limit locals 1


    aload_0
    getfield FindMaximum/_test_arr [I
    areturn

.end method

.method public static main([Ljava/lang/String;)V
    .limit stack 3
    .limit locals 2

    new FindMaximum
    dup
    invokespecial FindMaximum/<init>()V
    astore_1

    aload_1
    invokevirtual FindMaximum/build_test_arr()I
    pop

    aload_1
    aload_1
    invokevirtual FindMaximum/get_array()[I
    invokevirtual FindMaximum/find_maximum([I)I
    invokestatic ioPlus/printResult(I)V

    return

.end method
