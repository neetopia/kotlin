// FILE: a.kt

package com.bumptech.glide.annotation

annotation class GlideModule

// FILE: b.kt

package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class TachiGlideModule : AppGlideModule() {

}