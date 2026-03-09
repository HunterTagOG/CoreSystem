package dev.huntertagog.coresystem.platform.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandMeta {

    /**
     * Interner Name / Identifier (optional).
     */
    String value() default "";

    /**
     * Permission-Node (falls du ein Permission-System hast).
     */
    String permission() default "";

    /**
     * Feature-Toggle o.Ä.
     */
    boolean enabled() default true;
}
