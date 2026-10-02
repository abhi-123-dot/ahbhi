package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @param:Json(name = "contents") val contents: List<Content>,
    @param:Json(name = "systemInstruction") val systemInstruction: Content? = null,
    @param:Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @param:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @param:Json(name = "temperature") val temperature: Float? = null,
    @param:Json(name = "topP") val topP: Float? = null,
    @param:Json(name = "topK") val topK: Int? = null,
    @param:Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @param:Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @param:Json(name = "content") val content: Content? = null,
    @param:Json(name = "finishReason") val finishReason: String? = null
)
