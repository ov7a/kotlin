/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.declarations.IrFunction

interface RuntimeAware {
    val runtime: Runtime
}

class Runtime(bitcodeFile: String) {
    val llvmModule: LLVMModuleRef = parseBitcodeFile(bitcodeFile)
    val calculatedLLVMTypes: MutableMap<IrType, LLVMTypeRef> = HashMap()

    internal fun getStructTypeOrNull(name: String) = LLVMGetTypeByName(llvmModule, "struct.$name")
    internal fun getStructType(name: String) = getStructTypeOrNull(name)
            ?: throw Error("struct.$name is not found in the Runtime module.")

    val typeInfoType by lazy { getStructType("TypeInfo") }
    val extendedTypeInfoType by lazy { getStructType("ExtendedTypeInfo") }
    val writableTypeInfoType by lazy { getStructTypeOrNull("WritableTypeInfo") }
    val interfaceTableRecordType by lazy { getStructType("InterfaceTableRecord") }
    val associatedObjectTableRecordType by lazy { getStructType("AssociatedObjectTableRecord") }
    val kNodeInitType by lazy { getStructType("InitNode") }
    val kMemoryStateType by lazy { getStructType("MemoryState") }

    val objHeaderType by lazy { getStructType("ObjHeader") }
    val objHeaderPtrType by lazy { pointerType(objHeaderType) }
    val objHeaderPtrPtrType by lazy { pointerType(objHeaderType) }
    val arrayHeaderType by lazy { getStructType("ArrayHeader") }

    val frameOverlayType = getStructType("FrameOverlay")

    val target = LLVMGetTarget(llvmModule)!!.toKString()

    val dataLayout = LLVMGetDataLayout(llvmModule)!!.toKString()

    val targetData = LLVMCreateTargetData(dataLayout)!!

    val kotlinObjCClassData by lazy { getStructType("KotlinObjCClassData") }
    val kotlinObjCClassInfo by lazy { getStructType("KotlinObjCClassInfo") }
    val objCMethodDescription by lazy { getStructType("ObjCMethodDescription") }
    val objCTypeAdapter by lazy { getStructType("ObjCTypeAdapter") }
    val objCToKotlinMethodAdapter by lazy { getStructType("ObjCToKotlinMethodAdapter") }
    val kotlinToObjCMethodAdapter by lazy { getStructType("KotlinToObjCMethodAdapter") }
    val typeInfoObjCExportAddition by lazy { getStructType("TypeInfoObjCExportAddition") }

    val pointerSize: Int by lazy {
        LLVMABISizeOfType(targetData, objHeaderPtrType).toInt()
    }

    val pointerAlignment: Int by lazy {
        LLVMABIAlignmentOfType(targetData, objHeaderPtrType)
    }
}
