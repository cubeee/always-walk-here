package com.github.cubeee.alwayswalkhere;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode
class MarkedTile {
    int regionId;
    int regionX;
    int regionY;
    int z;
}
