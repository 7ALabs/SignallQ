package io.veloo.app.feature.diagnostico.topology

import io.veloo.app.feature.diagnostico.topology.correlation.NatClassifier
import io.veloo.app.feature.diagnostico.topology.model.NatStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NatClassifierTest {

    // --- isPrivate ---

    @Test
    fun `isPrivate retorna true para 10_0_0_1 (RFC1918 classe A)`() {
        assertTrue(NatClassifier.isPrivate("10.0.0.1"))
    }

    @Test
    fun `isPrivate retorna true para 172_16_0_1 (RFC1918 limite inferior B)`() {
        assertTrue(NatClassifier.isPrivate("172.16.0.1"))
    }

    @Test
    fun `isPrivate retorna true para 172_31_255_255 (RFC1918 limite superior B)`() {
        assertTrue(NatClassifier.isPrivate("172.31.255.255"))
    }

    @Test
    fun `isPrivate retorna true para 192_168_1_1 (RFC1918 classe C)`() {
        assertTrue(NatClassifier.isPrivate("192.168.1.1"))
    }

    @Test
    fun `isPrivate retorna false para 8_8_8_8 (IP publico)`() {
        assertFalse(NatClassifier.isPrivate("8.8.8.8"))
    }

    @Test
    fun `isPrivate retorna false para 172_32_0_1 (fora do range 172_16-31)`() {
        assertFalse(NatClassifier.isPrivate("172.32.0.1"))
    }

    @Test
    fun `isPrivate retorna false para 100_64_1_1 (CGNAT nao eh RFC1918)`() {
        assertFalse(NatClassifier.isPrivate("100.64.1.1"))
    }

    // --- isCgnatRange ---

    @Test
    fun `isCgnatRange retorna true para 100_64_0_1 (RFC6598 limite inferior)`() {
        assertTrue(NatClassifier.isCgnatRange("100.64.0.1"))
    }

    @Test
    fun `isCgnatRange retorna true para 100_127_255_255 (RFC6598 limite superior)`() {
        assertTrue(NatClassifier.isCgnatRange("100.127.255.255"))
    }

    @Test
    fun `isCgnatRange retorna false para 100_63_255_255 (abaixo do range)`() {
        assertFalse(NatClassifier.isCgnatRange("100.63.255.255"))
    }

    @Test
    fun `isCgnatRange retorna false para 100_128_0_0 (acima do range)`() {
        assertFalse(NatClassifier.isCgnatRange("100.128.0.0"))
    }

    @Test
    fun `isCgnatRange retorna false para 10_0_0_1 (RFC1918 nao eh CGNAT)`() {
        assertFalse(NatClassifier.isCgnatRange("10.0.0.1"))
    }

    // --- classify ---

    @Test
    fun `classify retorna UNKNOWN quando wanIp eh null`() {
        assertEquals(NatStatus.UNKNOWN, NatClassifier.classify(null, null))
    }

    @Test
    fun `classify retorna CGNAT quando wanIp esta no range CGNAT`() {
        assertEquals(NatStatus.CGNAT, NatClassifier.classify("100.64.0.1", null))
    }

    @Test
    fun `classify retorna DOUBLE_NAT_OR_CGNAT quando wanIp eh privado RFC1918`() {
        assertEquals(NatStatus.DOUBLE_NAT_OR_CGNAT, NatClassifier.classify("192.168.1.1", null))
    }

    @Test
    fun `classify retorna DIRECT_PUBLIC quando wanIp e publicIp sao iguais`() {
        assertEquals(NatStatus.DIRECT_PUBLIC, NatClassifier.classify("203.0.113.1", "203.0.113.1"))
    }

    @Test
    fun `classify retorna DOUBLE_NAT_OR_CGNAT quando wanIp e publicIp diferem`() {
        assertEquals(NatStatus.DOUBLE_NAT_OR_CGNAT, NatClassifier.classify("203.0.113.1", "203.0.113.2"))
    }

    @Test
    fun `classify retorna DIRECT_PUBLIC quando wanIp publico e publicIp null`() {
        assertEquals(NatStatus.DIRECT_PUBLIC, NatClassifier.classify("203.0.113.1", null))
    }
}
