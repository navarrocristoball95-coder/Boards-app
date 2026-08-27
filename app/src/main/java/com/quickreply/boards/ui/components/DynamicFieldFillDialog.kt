package com.quickreply.boards.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.quickreply.boards.ui.theme.BoardsBlue

data class DynamicVariable(
    val rawTag: String,
    val name: String,
    val defaultValue: String = "",
    val isCalculated: Boolean = false,
    val formula: String = ""
)

fun parseDynamicVariables(templateContent: String): List<DynamicVariable> {
    val regex = Regex("\\{([^}]+)\\}")
    return regex.findAll(templateContent).map { matchResult ->
        val raw = matchResult.groupValues[1]
        if (raw.contains("=")) {
            val parts = raw.split("=", limit = 2)
            val name = parts[0].trim()
            val formula = parts[1].trim()
            DynamicVariable(
                rawTag = raw,
                name = name,
                defaultValue = "",
                isCalculated = true,
                formula = formula
            )
        } else if (raw.contains(":")) {
            val parts = raw.split(":", limit = 2)
            val name = parts[0].trim()
            val defaultValue = parts[1].trim()
            DynamicVariable(
                rawTag = raw,
                name = name,
                defaultValue = defaultValue,
                isCalculated = false
            )
        } else {
            DynamicVariable(
                rawTag = raw,
                name = raw.trim(),
                defaultValue = "",
                isCalculated = false
            )
        }
    }.distinctBy { it.name }.toList()
}

fun calculateFormulas(
    variables: List<DynamicVariable>,
    fieldValues: MutableMap<String, String>
) {
    variables.filter { it.isCalculated }.forEach { calcVar ->
        val calculatedVal = evaluateFormula(calcVar.formula, fieldValues)
        if (calculatedVal.isNotBlank()) {
            fieldValues[calcVar.name] = calculatedVal
        }
    }
}

fun parseSmartNumber(raw: String): String {
    if (raw.isBlank()) return ""
    var text = raw.trim().replace("$", "").replace("€", "").replace("CLP", "").replace("UF", "").replace(" ", "").trim()
    
    if (text.contains(".") && text.contains(",")) {
        text = text.replace(".", "").replace(",", ".")
    } else if (text.contains(".") && !text.contains(",")) {
        val parts = text.split(".")
        if (parts.size > 2 || (parts.size == 2 && parts[1].length == 3)) {
            text = text.replace(".", "")
        }
    } else if (text.contains(",") && !text.contains(".")) {
        val parts = text.split(",")
        if (parts.size == 2 && parts[1].length != 3) {
            text = text.replace(",", ".")
        } else if (parts.size > 2 || (parts.size == 2 && parts[1].length == 3)) {
            text = text.replace(",", "")
        }
    }
    val cleanNum = text.filter { it.isDigit() || it == '.' || it == '-' }
    return cleanNum
}

fun evaluateFormula(formula: String, fieldValues: Map<String, String>): String {
    try {
        var expr = formula
        fieldValues.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                val cleanNum = parseSmartNumber(value)
                if (cleanNum.isNotBlank()) {
                    expr = expr.replace(Regex("\\b$key\\b"), cleanNum)
                }
            }
        }
        val result = evalSimpleMath(expr)
        return if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            "%.2f".format(result).replace(",", ".")
        }
    } catch (e: Exception) {
        return ""
    }
}

fun evalSimpleMath(expression: String): Double {
    var expr = expression.replace(" ", "")
    if (expr.isBlank()) return 0.0

    // 0. Resolver paréntesis de adentro hacia afuera
    while (expr.contains("(")) {
        val openIdx = expr.lastIndexOf('(')
        val closeIdx = expr.indexOf(')', openIdx)
        if (closeIdx == -1) break
        val inside = expr.substring(openIdx + 1, closeIdx)
        val insideVal = evalSimpleMath(inside)
        expr = expr.substring(0, openIdx) + insideVal.toString() + expr.substring(closeIdx + 1)
    }

    expr.toDoubleOrNull()?.let { return it }

    // 1. Suma y Resta (menor precedencia)
    for (i in expr.length - 1 downTo 1) {
        val c = expr[i]
        val prev = expr[i - 1]
        if (c == '+' && prev != '*' && prev != '/' && prev != '+' && prev != '-') {
            return evalSimpleMath(expr.substring(0, i)) + evalSimpleMath(expr.substring(i + 1))
        } else if (c == '-' && prev != '*' && prev != '/' && prev != '+' && prev != '-') {
            return evalSimpleMath(expr.substring(0, i)) - evalSimpleMath(expr.substring(i + 1))
        }
    }

    // 2. Multiplicación y División (mayor precedencia)
    for (i in expr.length - 1 downTo 1) {
        val c = expr[i]
        if (c == '*') {
            return evalSimpleMath(expr.substring(0, i)) * evalSimpleMath(expr.substring(i + 1))
        } else if (c == '/') {
            val denom = evalSimpleMath(expr.substring(i + 1))
            return if (denom != 0.0) evalSimpleMath(expr.substring(0, i)) / denom else 0.0
        }
    }

    return expr.toDoubleOrNull() ?: 0.0
}

fun processDynamicTemplate(
    templateContent: String,
    variables: List<DynamicVariable>,
    fieldValues: Map<String, String>
): String {
    var result = templateContent
    variables.forEach { variable ->
        val value = if (variable.isCalculated) {
            val calc = evaluateFormula(variable.formula, fieldValues)
            calc.ifBlank { variable.name }
        } else {
            fieldValues[variable.name]?.ifBlank { variable.defaultValue } ?: variable.defaultValue
        }
        result = result.replace("{${variable.rawTag}}", value)
    }
    return result
}

fun hasDynamicFields(content: String): Boolean {
    return Regex("\\{([^}]+)\\}").containsMatchIn(content)
}

@Composable
fun DynamicFieldFillDialog(
    templateContent: String,
    onDismiss: () -> Unit,
    onComplete: (processedText: String) -> Unit
) {
    val parsedVariables = remember(templateContent) {
        parseDynamicVariables(templateContent)
    }

    val fieldValues = remember {
        mutableStateMapOf<String, String>().apply {
            parsedVariables.forEach { variable ->
                if (!variable.isCalculated) {
                    this[variable.name] = variable.defaultValue
                }
            }
            calculateFormulas(parsedVariables, this)
        }
    }

    val nonCalculatedVariables = remember(parsedVariables) {
        parsedVariables.filter { !it.isCalculated }
    }

    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val currentPreview = remember(templateContent, fieldValues.toMap()) {
        processDynamicTemplate(templateContent, parsedVariables, fieldValues)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Personalizar Mensaje", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF191C20))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF707684))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Campos de Entrada
                Text(
                    text = "Ingresa los datos para personalizar:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475467)
                )
                Spacer(modifier = Modifier.height(10.dp))

                parsedVariables.forEachIndexed { index, variable ->
                    if (variable.isCalculated) {
                        val calculatedVal = evaluateFormula(variable.formula, fieldValues)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF3FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = BoardsBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${variable.name} (Calculado: ${variable.formula})",
                                        fontSize = 11.sp,
                                        color = BoardsBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = calculatedVal.ifBlank { "0" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF191C20)
                                    )
                                }
                            }
                        }
                    } else {
                        val isFirst = index == 0 || (nonCalculatedVariables.firstOrNull() == variable)
                        OutlinedTextField(
                            value = fieldValues[variable.name] ?: "",
                            onValueChange = { newValue ->
                                fieldValues[variable.name] = newValue
                                calculateFormulas(parsedVariables, fieldValues)
                            },
                            label = { Text("Valor para [${variable.name}]", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BoardsBlue,
                                unfocusedBorderColor = Color(0xFFE2E4E9)
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isFirst) Modifier.focusRequester(focusRequester) else Modifier)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Vista Previa en Vivo
                Text(
                    text = "Vista previa del mensaje:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475467)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF9FAFB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Text(
                        text = currentPreview.ifBlank { "(El mensaje aparecerá aquí...)" },
                        fontSize = 13.sp,
                        color = Color(0xFF191C20),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    calculateFormulas(parsedVariables, fieldValues)
                    val result = processDynamicTemplate(templateContent, parsedVariables, fieldValues)
                    onComplete(result)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BoardsBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Usar Mensaje", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF707684))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
