package com.eomer91.minigamesworld.data

import com.eomer91.minigamesworld.R

object GameCatalog {

    val games: List<Game> = listOf(
        Game("block-puzzle", R.string.game_block_puzzle_name, R.string.game_block_puzzle_desc, GameCategory.PUZZLE, R.drawable.game_block_puzzle, entryCost = 20, isFeatured = true),
        Game("match-3", R.string.game_match_3_name, R.string.game_match_3_desc, GameCategory.PUZZLE, R.drawable.game_match_3, entryCost = 25),
        Game("sudoku", R.string.game_sudoku_name, R.string.game_sudoku_desc, GameCategory.CLASSIC, R.drawable.game_sudoku, entryCost = 15),
        Game("memory-card", R.string.game_memory_card_name, R.string.game_memory_card_desc, GameCategory.CARD, R.drawable.game_memory_card, entryCost = 25),
        Game("stack", R.string.game_stack_name, R.string.game_stack_desc, GameCategory.ARCADE, R.drawable.game_stack, entryCost = 10),
        Game("bubble-shooter", R.string.game_bubble_shooter_name, R.string.game_bubble_shooter_desc, GameCategory.ARCADE, R.drawable.game_bubble_shooter, entryCost = 5),
        Game("2048", R.string.game_2048_name, R.string.game_2048_desc, GameCategory.PUZZLE, R.drawable.game_2048, entryCost = 10, isPlayable = true),
        Game("tower-defense", R.string.game_tower_defense_name, R.string.game_tower_defense_desc, GameCategory.STRATEGY, R.drawable.game_tower_defense, entryCost = 15),
        Game("snake", R.string.game_snake_name, R.string.game_snake_desc, GameCategory.ARCADE, R.drawable.game_snake, entryCost = 20, isPlayable = true),
    )

    fun find(id: String): Game? = games.firstOrNull { it.id == id }

    val featured: Game? get() = games.firstOrNull { it.isFeatured }
}
