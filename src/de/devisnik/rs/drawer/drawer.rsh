
typedef struct __attribute__((packed, aligned(4))) Tile {
    int2 position;
    rs_program_fragment texture;
} Tile_t;
