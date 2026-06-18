package buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinAssignmentWrappingTest {
    @Test
    fun formatsPropertyAssignmentFollowedByListFactory() {
        val input =
            """
            object NotesFeature {
                override val nativeDestinations =
                    listOf(
                        NativeNavigationDestination(id = "notes"),
                    )
            }
            """.trimIndent()

        val output = KotlinAssignmentWrapping.formatText(input, maxLineLength = 120)

        assertEquals(
            """
            object NotesFeature {
                override val nativeDestinations = listOf(
                    NativeNavigationDestination(id = "notes"),
                )
            }
            """.trimIndent(),
            output,
        )
    }

    @Test
    fun formatsNamedArgumentFollowedByConstructorCall() {
        val input =
            """
            DeepLinkSpec(
                pattern =
                    DeepLinkPattern(
                        scheme = "myapptemplate",
                    ),
            )
            """.trimIndent()

        val output = KotlinAssignmentWrapping.formatText(input, maxLineLength = 120)

        assertEquals(
            """
            DeepLinkSpec(
                pattern = DeepLinkPattern(
                    scheme = "myapptemplate",
                ),
            )
            """.trimIndent(),
            output,
        )
    }

    @Test
    fun leavesAssignmentSplitWhenCombinedLineWouldExceedLimit() {
        val input =
            """
            object Example {
                val veryLongPropertyNameThatAlreadyPushesTheLineCloseToTheConfiguredMaximum =
                    VeryLongFactoryNameThatWouldPushTheCombinedLinePastTheConfiguredMaximum(
                        value = 1,
                    )
            }
            """.trimIndent()

        val output = KotlinAssignmentWrapping.formatText(input, maxLineLength = 100)

        assertEquals(input, output)
    }

    @Test
    fun leavesFunctionExpressionBodySplitForKtlintFunctionSignatureRule() {
        val input =
            """
            private fun String.isDesktopDeepLinkCandidate(): Boolean =
                length <= MAX_DESKTOP_DEEP_LINK_LENGTH &&
                    startsWith("myapptemplate://")
            """.trimIndent()

        val output = KotlinAssignmentWrapping.formatText(input, maxLineLength = 120)

        assertEquals(input, output)
    }

    @Test
    fun leavesMultilineFunctionExpressionBodySplitForKtlintFunctionSignatureRule() {
        val input =
            """
            fun commandForUrl(
                url: String?,
                session: SessionState,
            ): NativeNavigationCommand? =
                registry.resolve(url)
            """.trimIndent()

        val output = KotlinAssignmentWrapping.formatText(input, maxLineLength = 120)

        assertEquals(input, output)
    }

    @Test
    fun reportsViolationLineNumbersForFormatableAssignments() {
        val input =
            """
            object NotesFeature {
                override val nativeDestinations =
                    listOf(
                        NativeNavigationDestination(id = "notes"),
                    )
            }
            """.trimIndent()

        val violations = KotlinAssignmentWrapping.findViolations(input, maxLineLength = 120)

        assertEquals(1, violations.size)
        assertEquals(2, violations.single().lineNumber)
        assertTrue(violations.single().replacement.contains("nativeDestinations = listOf("))
    }
}
