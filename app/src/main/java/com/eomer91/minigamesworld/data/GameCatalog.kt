package com.eomer91.minigamesworld.data

import com.eomer91.minigamesworld.R

object GameCatalog {

    /** Tum oyunlar (gizliler dahil). Listeleme icin [games] kullan. */
    val allGames: List<Game> = listOf(
        Game("block-puzzle", R.string.game_block_puzzle_name, R.string.game_block_puzzle_desc, GameCategory.PUZZLE, R.drawable.game_block_puzzle, isFeatured = true, isPlayable = true),
        Game("mahjong", R.string.game_mahjong_name, R.string.game_mahjong_desc, GameCategory.PUZZLE, R.drawable.game_mahjong, isPlayable = true, isHidden = true),
        Game("block-rain", R.string.game_block_rain_name, R.string.game_block_rain_desc, GameCategory.PUZZLE, R.drawable.game_block_rain, isPlayable = true),
        Game("jigsaw", R.string.game_jigsaw_name, R.string.game_jigsaw_desc, GameCategory.PUZZLE, R.drawable.game_jigsaw, isPlayable = true),
        Game("match-3", R.string.game_match_3_name, R.string.game_match_3_desc, GameCategory.PUZZLE, R.drawable.game_match_3, isPlayable = true),
        Game("sudoku", R.string.game_sudoku_name, R.string.game_sudoku_desc, GameCategory.CLASSIC, R.drawable.game_sudoku, isPlayable = true),
        Game("memory-card", R.string.game_memory_card_name, R.string.game_memory_card_desc, GameCategory.CARD, R.drawable.game_memory_card, isPlayable = true),
        Game("stack", R.string.game_stack_name, R.string.game_stack_desc, GameCategory.ARCADE, R.drawable.game_stack, isPlayable = true),
        Game("bubble-shooter", R.string.game_bubble_shooter_name, R.string.game_bubble_shooter_desc, GameCategory.ARCADE, R.drawable.game_bubble_shooter, isPlayable = true),
        Game("rocket-hop", R.string.game_rocket_hop_name, R.string.game_rocket_hop_desc, GameCategory.ARCADE, R.drawable.game_rocket_hop, isPlayable = true),
        Game("fish-world", R.string.game_fish_world_name, R.string.game_fish_world_desc, GameCategory.ARCADE, R.drawable.game_fish_world, isPlayable = true),
        Game("factory", R.string.game_factory_name, R.string.game_factory_desc, GameCategory.STRATEGY, R.drawable.game_factory, isPlayable = true),
        Game("island-tycoon", R.string.game_island_name, R.string.game_island_desc, GameCategory.STRATEGY, R.drawable.game_island, isPlayable = true),
        Game("football-manager", R.string.game_football_manager_name, R.string.game_football_manager_desc, GameCategory.STRATEGY, R.drawable.game_football_manager, isPlayable = true, isHidden = true),
        Game("stock-market", R.string.game_stock_market_name, R.string.game_stock_market_desc, GameCategory.STRATEGY, R.drawable.game_stock_market, isPlayable = true),
        Game("fox-adventure", R.string.game_fox_name, R.string.game_fox_desc, GameCategory.ARCADE, R.drawable.game_fox_adventure, isPlayable = true),
        Game("mine-run", R.string.game_mine_run_name, R.string.game_mine_run_desc, GameCategory.ARCADE, R.drawable.game_mine_run, isPlayable = true),
        Game("forest-guardian", R.string.game_forest_name, R.string.game_forest_desc, GameCategory.ARCADE, R.drawable.game_forest_guardian, isPlayable = true),
        Game("countries-war", R.string.game_countries_war_name, R.string.game_countries_war_desc, GameCategory.STRATEGY, R.drawable.game_countries_war, isPlayable = true),
        Game("dress-up", R.string.game_dress_up_name, R.string.game_dress_up_desc, GameCategory.CREATIVE, R.drawable.game_dress_up, isPlayable = true),
        Game("makeup", R.string.game_makeup_name, R.string.game_makeup_desc, GameCategory.CREATIVE, R.drawable.game_makeup, isPlayable = true),
        Game("cake-shop", R.string.game_cake_shop_name, R.string.game_cake_shop_desc, GameCategory.CREATIVE, R.drawable.game_cake_shop, isPlayable = true),
        Game("coloring", R.string.game_coloring_name, R.string.game_coloring_desc, GameCategory.CREATIVE, R.drawable.game_coloring, isPlayable = true),
        Game("animals", R.string.game_animals_name, R.string.game_animals_desc, GameCategory.CREATIVE, R.drawable.game_animals, isPlayable = true),
        Game("2048", R.string.game_2048_name, R.string.game_2048_desc, GameCategory.PUZZLE, R.drawable.game_2048, isPlayable = true),
        Game("chess", R.string.game_chess_name, R.string.game_chess_desc, GameCategory.STRATEGY, R.drawable.game_chess, isPlayable = true),
        Game("checkers", R.string.game_checkers_name, R.string.game_checkers_desc, GameCategory.STRATEGY, R.drawable.game_checkers, isPlayable = true),
        Game("pool", R.string.game_pool_name, R.string.game_pool_desc, GameCategory.CLASSIC, R.drawable.game_pool, isPlayable = true),
        Game("connect-four", R.string.game_connect_four_name, R.string.game_connect_four_desc, GameCategory.STRATEGY, R.drawable.game_connect_four, isPlayable = true),
        Game("tower-defense", R.string.game_tower_defense_name, R.string.game_tower_defense_desc, GameCategory.STRATEGY, R.drawable.game_tower_defense, isPlayable = true),
        Game("minesweeper", R.string.game_minesweeper_name, R.string.game_minesweeper_desc, GameCategory.CLASSIC, R.drawable.game_minesweeper, isPlayable = true),
        Game("snake", R.string.game_snake_name, R.string.game_snake_desc, GameCategory.ARCADE, R.drawable.game_snake, isPlayable = true),
    )

    /** Kullaniciya listelenen oyunlar: gizli olanlar haric. */
    val games: List<Game> = allGames.filter { !it.isHidden }

    fun find(id: String): Game? = allGames.firstOrNull { it.id == id }

    val featured: Game? get() = games.firstOrNull { it.isFeatured }
}
