package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString as composeGetString
import org.jetbrains.compose.resources.stringResource as composeStringResource

@Composable
fun stringResource(resource: StringResource) = composeStringResource(resource).escapeCharacters()

@Composable
fun stringResource(resource: StringResource, vararg formatArgs: Any) =
    composeStringResource(resource, *formatArgs).escapeCharacters()

suspend fun getString(resource: StringResource) = composeGetString(resource).escapeCharacters()

suspend fun getString(resource: StringResource, vararg formatArgs: Any) =
    composeGetString(resource, *formatArgs).escapeCharacters()

private fun String.escapeCharacters() = this
    .replace("%%", "%")
    .replace("\\\'", "\'")
    .replace("\\\"", "\"")
    .replace("\\?", "?")
