package com.eomer91.minigamesworld.data

import com.eomer91.minigamesworld.R

object GameCatalog {

    val games: List<Game> = listOf(
        Game("block-puzzle", R.string.game_block_puzzle_name, R.string.game_block_puzzle_desc, GameCategory.PUZZLE, R.drawable.game_block_puzzle, entryCost = 20, isFeatured = true, isPlayable = true),
        Game("block-rain", R.string.game_block_rain_name, R.string.game_block_rain_desc, GameCategory.PUZZLE, R.drawable.game_block_rain, entryCost = 10, isPlayable = true),
        Game("jigsaw", R.string.game_jigsaw_name, R.string.game_jigsaw_desc, GameCategory.PUZZLE, R.drawable.game_jigsaw, entryCost = 5, isPlayable = true),
        Game("match-3", R.string.game_match_3_name, R.string.game_match_3_desc, GameCategory.PUZZLE, R.drawable.game_match_3, entryCost = 25, isPlayable = true),
        Game("sudoku", R.string.game_sudoku_name, R.string.game_sudoku_desc, GameCategory.CLASSIC, R.drawable.game_sudoku, entryCost = 15, isPlayable = true),
        Game("memory-card", R.string.game_memory_card_name, R.string.game_memory_card_desc, GameCategory.CARD, R.drawable.game_memory_card, entryCost = 25, isPlayable = true),
        Game("solitaire", R.string.game_solitaire_name, R.string.game_solitaire_desc, GameCategory.CARD, R.drawable.game_solitaire, entryCost = 20, isPlayable = true),
        Game("stack", R.string.game_stack_name, R.string.game_stack_desc, GameCategory.ARCADE, R.drawable.game_stack, entryCost = 10, isPlayable = true),
        Game("bubble-shooter", R.string.game_bubble_shooter_name, R.string.game_bubble_shooter_desc, GameCategory.ARCADE, R.drawable.game_bubble_shooter, entryCost = 5, isPlayable = true),
        Game("rocket-hop", R.string.game_rocket_hop_name, R.string.game_rocket_hop_desc, GameCategory.ARCADE, R.drawable.game_rocket_hop, entryCost = 5, isPlayable = true),
        Game("fish-world", R.string.game_fish_world_name, R.string.game_fish_world_desc, GameCategory.ARCADE, R.drawable.game_fish_world, entryCost = 5, isPlayable = true),
        Game("stock-market", R.string.game_stock_market_name, R.string.game_stock_market_desc, GameCategory.STRATEGY, R.drawable.game_stock_market, entryCost = 5, isPlayable = true),
        Game("fox-adventure", R.string.game_fox_name, R.string.game_fox_desc, GameCategory.ARCADE, R.drawable.game_fox_adventure, entryCost = 5, isPlayable = true),
        Game("forest-guardian", R.string.game_forest_name, R.string.game_forest_desc, GameCategory.ARCADE, R.drawable.game_forest_guardian, entryCost = 5, isPlayable = true),
        Game("countries-war", R.string.game_countries_war_name, R.string.game_countries_war_desc, GameCategory.STRATEGY, R.drawable.game_countries_war, entryCost = 5, isPlayable = true),
        Game("dress-up", R.string.game_dress_up_name, R.string.game_dress_up_desc, GameCategory.CREATIVE, R.drawable.game_dress_up, entryCost = 5, isPlayable = true),
        Game("makeup", R.string.game_makeup_name, R.string.game_makeup_desc, GameCategory.CREATIVE, R.drawable.game_makeup, entryCost = 5, isPlayable = true),
        Game("cake-shop", R.string.game_cake_shop_name, R.string.game_cake_shop_desc, GameCategory.CREATIVE, R.drawable.game_cake_shop, entryCost = 5, isPlayable = true),
        Game("2048", R.string.game_2048_name, R.string.game_2048_desc, GameCategory.PUZZLE, R.drawable.game_2048, entryCost = 10, isPlayable = true),
        Game("chess", R.string.game_chess_name, R.string.game_chess_desc, GameCategory.STRATEGY, R.drawable.game_chess, entryCost = 10, isPlayable = true),
        Game("checkers", R.string.game_checkers_name, R.string.game_checkers_desc, GameCategory.STRATEGY, R.drawable.game_checkers, entryCost = 10, isPlayable = true),
        Game("connect-four", R.string.game_connect_four_name, R.string.game_connect_four_desc, GameCategory.STRATEGY, R.drawable.game_connect_four, entryCost = 10, isPlayable = true),
        Game("tower-defense", R.string.game_tower_defense_name, R.string.game_tower_defense_desc, GameCategory.STRATEGY, R.drawable.game_tower_defense, entryCost = 15, isPlayable = true),
        Game("minesweeper", R.string.game_minesweeper_name, R.string.game_minesweeper_desc, GameCategory.CLASSIC, R.drawable.game_minesweeper, entryCost = 10, isPlayable = true),
        Game("snake", R.string.game_snake_name, R.string.game_snake_desc, GameCategory.ARCADE, R.drawable.game_snake, entryCost = 20, isPlayable = true),
    )

    fun find(id: String): Game? = games.firstOrNull { it.id == id }

    val featured: Game? get() = games.firstOrNull { it.isFeatured }
}
