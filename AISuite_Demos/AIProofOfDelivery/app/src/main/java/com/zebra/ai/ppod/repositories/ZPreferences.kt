package com.zebra.ai.ppod.repositories

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_CONFIGURATION_CHANGED
import android.content.IntentFilter
import android.content.RestrictionsManager
import android.content.SharedPreferences
import android.content.res.XmlResourceParser
import android.os.Bundle
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.xmlpull.v1.XmlPullParser

/***********************************************************************************/
enum class PreferenceType {
    CATEGORY,
    BOOL,
    STRING,
    INTEGER,
    FLOAT,
    CHOICE,
}
/***********************************************************************************/
data class PreferenceItem(
    val type: PreferenceType,
    val key: String,
    val title: String,
    val description: String?,
    val value: Any? = null,
    val restrictedValue: Any? = null,
    val minValue: Any? = null,
    val maxValue: Any? = null,
    val decimalPoints: Int = 2,
    val hidden: Boolean = false,
    val restricted: Boolean = false,
    val entries: List<Pair<String, String>>? = null,
)
/***********************************************************************************/
fun interface OnPreferenceChangedListener {
    fun onPreferencesChanged()
}
/***********************************************************************************/
private data class ListenerRegistration(
    val keys: Set<String>,
    val listener: OnPreferenceChangedListener
)
/***********************************************************************************/
class ZPreferences(context: Context, resourceXml: Int) {
    private val _sharedPreferences: SharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    private val _preferenceMap: MutableMap<String, PreferenceItem> = mutableMapOf()
    private val _allPreferenceItems: MutableList<PreferenceItem> = mutableListOf()
    private val _preferenceItemsFlow = MutableStateFlow<List<PreferenceItem>>(emptyList())
    private val _listeners = mutableListOf<ListenerRegistration>()

    init {
        loadFromXml(context, resourceXml)
        loadFromSharedPreferences()
        applyAppRestrictions(context)
        syncFlow()
    }
    /**************************************************************************************************/
    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.let {
                applyAppRestrictions(it)
                syncFlow()
            }
        }
    }
    /**************************************************************************************************/
    private fun loadFromXml(context: Context, resourceXml: Int) {
        val xml = context.resources.getXml(resourceXml)
        var eventType = xml.eventType
        var categoryCount = 0

        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (xml.name) {
                        "category" -> {
                            val item = PreferenceItem(
                                type = PreferenceType.CATEGORY,
                                key = "category_${categoryCount++}",
                                title = resolveString(context, xml, "title"),
                                description = resolveString(context, xml, "description")
                            )
                            _allPreferenceItems.add(item)
                        }
                        "item" -> {
                            val typeStr = xml.getAttributeValue(null, "type")
                            val item = when (typeStr) {
                                "bool" -> processBoolean(context, xml)
                                "choice" -> processChoice(context, xml)
                                "float" -> processFloat(context, xml)
                                "string" -> processString(context, xml)
                                "integer" -> processInteger(context, xml)
                                else -> null
                            }
                            item?.let {
                                _preferenceMap[it.key] = it
                                _allPreferenceItems.add(it)
                            }
                        }
                    }
                }
                eventType = xml.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    /**************************************************************************************************/
    private fun loadFromSharedPreferences() {
        val allSaved = _sharedPreferences.all
        _preferenceMap.forEach { (key, item) ->
            val savedValue = allSaved[key] ?: return@forEach
            val typedValue = try {
                when (item.type) {
                    PreferenceType.BOOL -> savedValue as? Boolean
                    PreferenceType.CHOICE -> savedValue as? String
                    PreferenceType.FLOAT -> {
                        when (savedValue) {
                            is Float -> savedValue
                            is Int -> savedValue.toFloat()
                            is String -> savedValue.toFloatOrNull()
                            else -> null
                        }
                    }
                    PreferenceType.STRING -> savedValue as? String
                    PreferenceType.INTEGER -> {
                        when (savedValue) {
                            is Int -> savedValue
                            is Long -> savedValue.toInt()
                            is String -> savedValue.toIntOrNull()
                            else -> null
                        }
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }

            if (typedValue != null) {
                val updated = item.copy(value = typedValue)
                _preferenceMap[key] = updated
                val idx = _allPreferenceItems.indexOfFirst { it.key == key }
                if (idx != -1) _allPreferenceItems[idx] = updated
            }
        }
    }
    /**************************************************************************************************/
    private fun processBoolean(context: Context, xml: XmlResourceParser): PreferenceItem? {
        val key = xml.getAttributeValue(null, "key") ?: return null
        return PreferenceItem(
            type = PreferenceType.BOOL,
            key = key,
            title = resolveString(context, xml, "title"),
            description = resolveString(context, xml, "description"),
            hidden = resolveBoolean(xml, "hidden"),
            value = resolveBoolean(xml, "default")
        )
    }
    /**************************************************************************************************/
    private fun processChoice(context: Context, xml: XmlResourceParser): PreferenceItem? {
        val key = xml.getAttributeValue(null, "key") ?: return null
        val entries = resolveStringArray(context, xml, "entries")
        val values = resolveStringArray(context, xml, "entryValues")
        return PreferenceItem(
            type = PreferenceType.CHOICE,
            key = key,
            title = resolveString(context, xml, "title"),
            description = resolveString(context, xml, "description"),
            hidden = resolveBoolean(xml, "hidden"),
            entries = entries.zip(values),
            value = resolveString(context, xml, "default")
        )
    }
    /**************************************************************************************************/
    private fun processFloat(context: Context, xml: XmlResourceParser): PreferenceItem? {
        val key = xml.getAttributeValue(null, "key") ?: return null
        return PreferenceItem(
            type = PreferenceType.FLOAT,
            key = key,
            title = resolveString(context, xml, "title"),
            description = resolveString(context, xml, "description"),
            hidden = resolveBoolean(xml, "hidden"),
            value = resolveFloat(context, xml, "default"),
            minValue = resolveFloat(context, xml, "minValue"),
            maxValue = resolveFloat(context, xml, "maxValue"),
            decimalPoints = xml.getAttributeIntValue(null, "decimalPoints", 2)
        )
    }
    /**************************************************************************************************/
    private fun processString(context: Context, xml: XmlResourceParser): PreferenceItem? {
        val key = xml.getAttributeValue(null, "key") ?: return null
        return PreferenceItem(
            type = PreferenceType.STRING,
            key = key,
            title = resolveString(context, xml, "title"),
            description = resolveString(context, xml, "description"),
            hidden = resolveBoolean(xml, "hidden"),
            value = resolveString(context, xml, "default")
        )
    }
    /**************************************************************************************************/
    private fun processInteger(context: Context, xml: XmlResourceParser): PreferenceItem? {
        val key = xml.getAttributeValue(null, "key") ?: return null
        return PreferenceItem(
            type = PreferenceType.INTEGER,
            key = key,
            title = resolveString(context, xml, "title"),
            description = resolveString(context, xml, "description"),
            hidden = resolveBoolean(xml, "hidden"),
            value = resolveInt(context, xml, "default"),
            minValue = resolveInt(context, xml, "minValue"),
            maxValue = resolveInt(context, xml, "maxValue")
        )
    }
    /**************************************************************************************************/
    fun registerConfigReceiver(context: Context) {
        context.registerReceiver(configReceiver, IntentFilter(ACTION_CONFIGURATION_CHANGED))
    }
    /**************************************************************************************************/
    fun unregisterConfigReceiver(context: Context) {
        context.unregisterReceiver(configReceiver)
    }
    /**************************************************************************************************/
    fun addPreferenceListener(keys: List<String>, listener: OnPreferenceChangedListener) {
        _listeners.add(ListenerRegistration(keys.toSet(), listener))
    }
    /**************************************************************************************************/
    fun removePreferenceListener(listener: OnPreferenceChangedListener) {
        _listeners.removeAll { it.listener == listener }
    }
    /**************************************************************************************************/
    fun getPreferenceItems(): StateFlow<List<PreferenceItem>> = _preferenceItemsFlow.asStateFlow()
    /**************************************************************************************************/
    operator fun set(property: String, value: Any) {
        val index = _allPreferenceItems.indexOfFirst { it.key == property }
        if (index != -1) {
            val item = _allPreferenceItems[index]
            _allPreferenceItems[index] = item.copy(
                value = value
            )
        }
        syncFlow()
    }
    /**************************************************************************************************/
    operator fun get(property: String): Any? {
        return if (_preferenceMap[property]?.restricted == true) _preferenceMap[property]?.restrictedValue else _preferenceMap[property]?.value
    }
    /**************************************************************************************************/
    fun commit() {
        val changedKeys = mutableSetOf<String>()
        _sharedPreferences.edit(commit = true) {
            _allPreferenceItems.forEach { item ->
                if (item.type == PreferenceType.CATEGORY) return@forEach
                val oldValue = _preferenceMap[item.key]?.value
                val newValue = item.value
                if (oldValue != newValue) changedKeys.add(item.key)
                if (newValue != null) {
                    when (item.type) {
                        PreferenceType.BOOL -> putBoolean(item.key, newValue as Boolean)
                        PreferenceType.CHOICE -> putString(item.key, newValue as String)
                        PreferenceType.FLOAT -> putFloat(item.key, newValue as Float)
                        PreferenceType.STRING -> putString(item.key, newValue as String)
                        PreferenceType.INTEGER -> putInt(item.key, newValue as Int)
                    }
                }
                _preferenceMap[item.key] = item
            }
        }

        // Notify Listeners
        if (changedKeys.isNotEmpty()) {
            _listeners.forEach { registration ->
                if (registration.keys.any { it in changedKeys }) {
                    registration.listener.onPreferencesChanged()
                }
            }
        }
    }
    /**************************************************************************************************/
    fun rollback() {
        _allPreferenceItems.forEachIndexed { index, item ->
            _preferenceMap[item.key]?.let { committedItem ->
                _allPreferenceItems[index] = item.copy(
                    value = committedItem.value,
                    restricted = committedItem.restricted
                )
            }
        }
        syncFlow()
    }
    /**************************************************************************************************/
    private fun applyAppRestrictions(context: Context) {
        val rm = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val restrictions: Bundle? = rm.applicationRestrictions

        _preferenceMap.forEach { (key, item) ->
            if (restrictions?.containsKey(key) == true) {
                val restrictedValue: Any? = when (item.type) {
                    PreferenceType.BOOL -> restrictions.getBoolean(key)
                    PreferenceType.CHOICE -> restrictions.getString(key)
                    PreferenceType.FLOAT -> restrictions.getFloat(key)
                    PreferenceType.INTEGER -> restrictions.getInt(key)
                    PreferenceType.STRING -> restrictions.getString(key)
                    else -> null
                }

                restrictedValue?.let {
                    val updated = item.copy(restrictedValue = it, restricted = true)
                    _preferenceMap[key] = updated
                    val idx = _allPreferenceItems.indexOfFirst { item -> item.key == key }
                    if (idx != -1) _allPreferenceItems[idx] = updated
                }
            }else{
                if (item.restricted) {
                    val updated = item.copy(restricted = false)
                    _preferenceMap[key] = updated
                    val idx = _allPreferenceItems.indexOfFirst { it.key == key }
                    if (idx != -1) _allPreferenceItems[idx] = updated
                }
            }
        }
    }
    /**************************************************************************************************/
    private fun syncFlow() {
        _preferenceItemsFlow.value = _allPreferenceItems.toList()
    }
    /**************************************************************************************************/
    private fun resolveString(context: Context, xml: XmlResourceParser, name: String): String {
        val resId = xml.getAttributeResourceValue(null, name, 0)
        return if (resId != 0) context.resources.getString(resId) else xml.getAttributeValue(null, name) ?: ""
    }
    /**************************************************************************************************/
    private fun resolveStringArray(context: Context, xml: XmlResourceParser, name: String): Array<String> {
        val resId = xml.getAttributeResourceValue(null, name, 0)
        return if (resId != 0) context.resources.getStringArray(resId) else emptyArray()
    }
    /**************************************************************************************************/
    private fun resolveBoolean(xml: XmlResourceParser, name: String): Boolean {
        return xml.getAttributeBooleanValue(null, name, false)
    }
    /**************************************************************************************************/
    private fun resolveInt(context: Context, xml: XmlResourceParser, name: String): Int? {
        val resId = xml.getAttributeResourceValue(null, name, 0)
        if (resId != 0) return context.resources.getInteger(resId)
        val attrValue = xml.getAttributeValue(null, name) ?: return null
        return attrValue.toIntOrNull()
    }
    /**************************************************************************************************/
    private fun resolveFloat(context: Context, xml: XmlResourceParser, name: String): Float? {
        val resId = xml.getAttributeResourceValue(null, name, 0)
        if (resId != 0) {
            return try {
                context.resources.getString(resId).toFloatOrNull()
            } catch (_: Exception) {
                null
            }
        }
        val attrValue = xml.getAttributeValue(null, name) ?: return null
        return attrValue.toFloatOrNull()
    }
}
