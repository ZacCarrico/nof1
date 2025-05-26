package com.nof1.data.model

data class LlmRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val max_tokens: Int = 500,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class LlmResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)