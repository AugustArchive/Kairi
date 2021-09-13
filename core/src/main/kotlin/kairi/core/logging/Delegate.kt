package kairi.core.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal class Slf4jDelegate(private val klass: KClass<*>): ReadOnlyProperty<Any?, Logger> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Logger = LoggerFactory.getLogger(klass.java)
}

internal inline fun <reified T> logging(): Slf4jDelegate = Slf4jDelegate(T::class)
