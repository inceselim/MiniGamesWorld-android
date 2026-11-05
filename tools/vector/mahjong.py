"""Mahjong tas yuzleri -> MahjongArt.kt. Kullanim: python3 mahjong.py [--preview]"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mj_classic, mj_jade, mj_ocean, mj_forest, mj_fruit, mj_night

SETS = [('classic', mj_classic), ('jade', mj_jade), ('ocean', mj_ocean), ('forest', mj_forest), ('fruit', mj_fruit), ('night', mj_night)]
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../app/src/main/java/com/eomer91/minigamesworld/ui/games/mahjong/MahjongArt.kt')

def kt():
    L = ['package com.eomer91.minigamesworld.ui.games.mahjong', '', 'import com.eomer91.minigamesworld.ui.games.common.VP', '',
         '// URETILDI: tools/vector/mahjong.py — elle duzenleme.',
         '// Tas yuzu viewBox 100 x 130. Her set bir tema (bolum) icindir.', '',
         'object MahjongArt {',
         '    /** Tema anahtari -> yuz listesi. Ilk erisimde olusturulur. */',
         '    val sets: Map<String, List<List<VP>>> by lazy {',
         '        mapOf(' + ', '.join(f'"{k}" to {k}()' for k, _ in SETS) + ')',
         '    }', '']
    for k, m in SETS:
        faces = m.faces()
        # JVM metot boyutu siniri icin her yuz ayri fonksiyon
        for i, f in enumerate(faces):
            L.append(f'    private fun {k}{i}(): List<VP> = listOf(')
            for d, fill, st in f:
                L.append(f'        VP("{d}", "{fill}"{", false" if not st else ""}),')
            L.append('    )')
        L.append(f'    private fun {k}(): List<List<VP>> = listOf(' + ', '.join(f'{k}{i}()' for i in range(len(faces))) + ')')
        L.append('')
    L.append('}')
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, 'w').write("\n".join(L) + "\n")
    print('faces:', {k: len(m.faces()) for k, m in SETS}, 'bytes:', os.path.getsize(OUT))

if __name__ == '__main__':
    if '--preview' in sys.argv:
        from mj_preview import sheet, THEMES
        for k, m in SETS: sheet(m.faces(), THEMES[k], 9, f'/tmp/mj_{k}.png')
    kt()
