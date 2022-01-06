package com.example.azizlumiere

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.stream.Collectors
import kotlin.collections.HashMap

private const val PROFILES_DIR_NAME = "profiles"

class ProfileManager(private val context: Context) {

    var profiles = HashMap<String, Profile>()
        private set

    fun addProfile(uri: Uri): Profile? {
        val resolver = context.contentResolver
        val stream = resolver.openInputStream(uri) ?: return null
        val buf = stream.bufferedReader()
        val content = parseBuffer(buf)
        val name = getUriFilename(uri, resolver)
        val profile = Profile(name, content)
        profiles[profile.name] = profile
        writeProfileToInternal(profile)
        return profile
    }

    private fun writeProfileToInternal(profile: Profile) {
        val profilesDirHandle = File(context.filesDir, PROFILES_DIR_NAME)
        val newFile = File(profilesDirHandle, profile.name)
        val writer = FileWriter(newFile)
        val content = profile.data.joinToString("\n") { entry ->
            "${entry.illumination} ${entry.brightness}"
        }
        writer.append(content)
        writer.flush()
        writer.close()
    }

    private fun getUriFilename(uri: Uri, resolver: ContentResolver): String {
        val cursor = resolver.query(uri, null, null, null) ?: return "unknown.txt"
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        val name = cursor.getString(nameIndex)
        cursor.close()
        return name
    }


    companion object {
        fun loadProfile(context: Context, name: String): Profile? {
            val profilesDirHandle = File(context.filesDir, PROFILES_DIR_NAME)
            val profileFile = File(profilesDirHandle, name)
            if (!profileFile.exists()) {
                return null
            }
            val buf = BufferedReader(FileReader(profileFile))
            val content = parseBuffer(buf)
            return Profile(profileFile.name, content)
        }

        private fun parseBuffer(buf: BufferedReader): List<ProfileDataPoint> {
            return buf.lines()
                .map lines@{ line ->
                    val data = line.split(" ")
                    val rawLux = data[0]
                    val rawBrightness = data[1]
                    val parsedLux = rawLux.toFloatOrNull() ?: return@lines null
                    val parsedBrightness = rawBrightness.toIntOrNull() ?: return@lines null
                    ProfileDataPoint(parsedLux, parsedBrightness)
                }
                .collect(Collectors.toList())
                .filterNotNull()
        }
    }

    fun loadProfiles() {
        val profilesDirHandle = File(context.filesDir, PROFILES_DIR_NAME)
        if (!profilesDirHandle.exists()) {
            profilesDirHandle.mkdir()
            return
        }
        val profilesList = profilesDirHandle.listFiles()
            ?.map file@{ file ->
                val buf = BufferedReader(FileReader(file))
                val content = parseBuffer(buf)
                Profile(file.name, content)
            }
        if (profilesList != null) {
            profiles.putAll(
                profilesList
                    .associateBy({ it.name }, { it })
            )
        }
    }
}