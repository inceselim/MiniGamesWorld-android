# Tilki Macerası bölümleri

```
python3 fox_levels.py        # FoxLevels.kt'yi yeniden üretir (her bölümde 3 yıldız kontrolü)
```

Bölümler parçalardan (chunk) birleştirilir; karakterler `fox_levels.py`'nin başında açıklanmıştır.

## Bitirilebilirlik kontrolü

`FoxSolver.kt` gerçek oyun motoruyla (FoxEngine) arama yapar: her bölümün **hasar almadan** bitirilebildiğini
ve her yıldızın alınıp bölüme devam edilebildiğini doğrular. Kotlin derleyicisi ile:

```
D=app/src/main/java/com/eomer91/minigamesworld/ui/games/fox
kotlinc $D/FoxEngine.kt $D/FoxLevels.kt tools/levels/FoxSolver.kt -d /tmp/solver.jar
java -cp /tmp/solver.jar:$KOTLIN_HOME/lib/kotlin-stdlib.jar com.eomer91.minigamesworld.ui.games.fox.FoxSolverKt
```
