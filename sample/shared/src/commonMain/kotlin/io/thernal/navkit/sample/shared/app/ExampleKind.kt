package io.thernal.navkit.sample.shared.app

/**
 * Whether an example is the smallest thing that works, or the shape a real screen would have. Declared in
 * catalog order: the simple example of a group comes before its advanced sibling.
 */
enum class ExampleKind(val label: String) {
    SIMPLE("simple"),
    ADVANCED("advanced"),
}
