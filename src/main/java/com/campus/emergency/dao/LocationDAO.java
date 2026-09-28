package com.campus.emergency.dao;

import com.campus.emergency.model.Location;
import java.util.List;
import java.util.Optional;

public interface LocationDAO {
    Location create(Location location);
    Optional<Location> findById(int id);
    List<Location> findAll();
}
