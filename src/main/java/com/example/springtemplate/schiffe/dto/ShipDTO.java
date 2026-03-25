package com.example.springtemplate.schiffe.dto;


import com.example.springtemplate.schiffe.model.Ship;
import com.example.springtemplate.schiffe.model.enums.ShipType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ShipDTO {

    private ShipType type;
    private String displayName;
    private int startX;
    private int startY;
    private boolean horizontal;
    private int size;
    private int hits;
    private boolean sunk;

    public ShipDTO() {}

    public static ShipDTO from(Ship ship) {
        ShipDTO dto = new ShipDTO();
        dto.type = ship.getType();
        dto.displayName = ship.getType().getDisplayName();
        dto.startX = ship.getStartX();
        dto.startY = ship.getStartY();
        dto.horizontal = ship.isHorizontal();
        dto.size = ship.getType().getSize();
        dto.hits = ship.getHits();
        dto.sunk = ship.isSunk();
        return dto;
    }

}