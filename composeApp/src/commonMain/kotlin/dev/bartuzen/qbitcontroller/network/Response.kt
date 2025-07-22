package dev.bartuzen.qbitcontroller.network

class Response<T>(
    val code: Int,
    val body: T?,
)
