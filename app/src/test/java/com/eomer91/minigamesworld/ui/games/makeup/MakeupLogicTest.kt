package com.eomer91.minigamesworld.ui.games.makeup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MakeupLogicTest {

    /** Bolgenin uzerinde ileri geri ov. */
    private fun rubRegion(face: FaceMakeup, tool: MakeupTool, color: Long, r: FaceRegion, passes: Int = 3): FaceMakeup {
        var f = face
        repeat(passes) {
            var x = r.cx - r.rx * 0.8f
            while (x < r.cx + r.rx * 0.8f) {
                f = Makeup.rub(f, tool, color, x, r.cy, 4f)
                x += 4f
            }
        }
        return f
    }

    @Test
    fun rubbingBuildsCoverageOnlyInsideRegion() {
        val c = MakeupPalettes.lips[0]
        val f = rubRegion(FaceMakeup(), MakeupTool.LIPS, c, FaceRegion.LIPS)
        assertTrue(f.paint(FaceRegion.LIPS).coverage > 0.6f)
        assertEquals(c, f.paint(FaceRegion.LIPS).color)
        // Ruj gozlere surulmez
        val g = Makeup.rub(FaceMakeup(), MakeupTool.LIPS, c, 113f, 158f, 20f)
        assertEquals(0f, g.paint(FaceRegion.SHADOW_L).coverage)
    }

    @Test
    fun newColorBlendsOverOld() {
        val a = MakeupPalettes.shadow[0]
        val b = MakeupPalettes.shadow[3]
        var f = rubRegion(FaceMakeup(), MakeupTool.SHADOW, a, FaceRegion.SHADOW_L)
        f = rubRegion(f, MakeupTool.SHADOW, b, FaceRegion.SHADOW_L)
        assertTrue(Makeup.colorDistance(f.paint(FaceRegion.SHADOW_L).color, b) < 20f)
    }

    @Test
    fun wipeRemovesMakeup() {
        var f = rubRegion(FaceMakeup(), MakeupTool.BLUSH, MakeupPalettes.blush[0], FaceRegion.BLUSH_L)
        f = rubRegion(f, MakeupTool.WIPE, 0, FaceRegion.BLUSH_L, passes = 5)
        assertEquals(0f, f.paint(FaceRegion.BLUSH_L).coverage, 0.001f)
    }

    @Test
    fun perfectLookGetsThreeStars() {
        val req = MakeupRequest(MakeupPalettes.shadow[1], MakeupPalettes.blush[0], MakeupPalettes.lips[2], 2)
        var f = FaceMakeup(lashes = 2)
        f = rubRegion(f, MakeupTool.SHADOW, req.shadow!!, FaceRegion.SHADOW_L)
        f = rubRegion(f, MakeupTool.SHADOW, req.shadow!!, FaceRegion.SHADOW_R)
        f = rubRegion(f, MakeupTool.BLUSH, req.blush!!, FaceRegion.BLUSH_L)
        f = rubRegion(f, MakeupTool.BLUSH, req.blush!!, FaceRegion.BLUSH_R)
        f = rubRegion(f, MakeupTool.LIPS, req.lips!!, FaceRegion.LIPS)
        assertEquals(3, Makeup.stars(Makeup.match(f, req)))
        // Bos yuz dusuk puan alir
        assertTrue(Makeup.stars(Makeup.match(FaceMakeup(), req)) <= 1)
        // Yanlis renk daha dusuk
        var wrong = FaceMakeup(lashes = 2)
        wrong = rubRegion(wrong, MakeupTool.LIPS, MakeupPalettes.lips[7], FaceRegion.LIPS)
        assertTrue(Makeup.match(wrong, req) < Makeup.match(f, req))
    }

    @Test
    fun noMakeupRequestRewardsCleanRegion() {
        val req = MakeupRequest(shadow = null, blush = null, lips = MakeupPalettes.lips[0], lashes = 0)
        var f = FaceMakeup()
        f = rubRegion(f, MakeupTool.LIPS, MakeupPalettes.lips[0], FaceRegion.LIPS)
        assertEquals(3, Makeup.stars(Makeup.match(f, req)))
    }
}
