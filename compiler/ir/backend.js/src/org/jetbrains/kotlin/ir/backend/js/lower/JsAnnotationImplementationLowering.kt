/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.render

class JsAnnotationImplementationTransformer(val jsContext: JsIrBackendContext, irFile: IrFile): AnnotationImplementationTransformer(jsContext, irFile) {

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        // No-op
        return expression
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (!declaration.isAnnotationClass) return declaration
        val properties = declaration.getAnnotationProperties()
        implementEqualsAndHashCode(declaration, declaration, properties, properties)
        return declaration
    }

    private val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol> = jsContext.ir.symbols.arraysContentEquals

    override fun generatedEquals(irBuilder: IrBlockBodyBuilder, type: IrType, arg1: IrExpression, arg2: IrExpression): IrExpression {
        return if (type.isArray() || type.isPrimitiveArray()) {
            val requiredSymbol =
                if (type.isPrimitiveArray())
                    arraysContentEquals[type]
                else
                    arraysContentEquals.entries.single { (k, _) -> k.isArray() }.value
            requireNotNull(requiredSymbol) { "Can't find contentEquals method for type ${type.render()}" }
            irBuilder.irCall(
                requiredSymbol
            ).apply {
                extensionReceiver = arg1
                putValueArgument(0, arg2)
            }
        } else super.generatedEquals(irBuilder, type, arg1, arg2)
    }
}