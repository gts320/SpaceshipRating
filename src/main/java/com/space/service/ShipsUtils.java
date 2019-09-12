package com.space.service;

import com.space.model.Ship;
import com.space.exceptions.Bad_Request_Exception;
import com.space.model.ShipType;

import org.springframework.stereotype.Service;

import java.util.*;

//Вспомагательный класс

@Service
public class ShipsUtils {
    //Фильтрация списка кораблей
    static List<Ship> filterShips(List<Ship> shipsToFiltered,
                                  String name,
                                  String planet,
                                  String shipType,
                                  Long after,
                                  Long before,
                                  Boolean isUsed,
                                  Double minSpeed,
                                  Double maxSpeed,
                                  Integer minCrewSize,
                                  Integer maxCrewSize,
                                  Double minRating,
                                  Double maxRating) {

//Формируем список, удаляя из него корабли, не отвечающие условиям
        shipsToFiltered.removeIf(ship ->
                (Objects.nonNull(name) && !ship.getName().toLowerCase().contains(name.toLowerCase()))
                        || (Objects.nonNull(planet) && !ship.getPlanet().toLowerCase().contains(planet.toLowerCase()))
                        || (Objects.nonNull(shipType) && ship.getShipType() != ShipType.valueOf(shipType))
                        || (Objects.nonNull(after) && (!ship.getProdDate().after(new Date(after)) && !Objects.equals(ship.getProdDate(), new Date(after))))
                        || (Objects.nonNull(before) && (!ship.getProdDate().before(new Date(before)) && !Objects.equals(ship.getProdDate(), new Date(before))))
                        || (Objects.nonNull(isUsed) && !ship.isUsed().equals(isUsed))
                        || (Objects.nonNull(minSpeed) && ship.getSpeed() < minSpeed)
                        || (Objects.nonNull(maxSpeed) && ship.getSpeed() > maxSpeed)
                        || (Objects.nonNull(minCrewSize) && ship.getCrewSize() < minCrewSize)
                        || (Objects.nonNull(maxCrewSize) && ship.getCrewSize() > maxCrewSize)
                        || (Objects.nonNull(minRating) && ship.getRating() < minRating)
                        || (Objects.nonNull(maxRating) && ship.getRating() > maxRating)
        );

        return shipsToFiltered;
    }

    //Проверка параметров корабля на корректность
    static void validateParameters(Ship ship) {
        if (Objects.isNull(ship.getName())
                || Objects.isNull(ship.getPlanet())
                || Objects.isNull(ship.getShipType())
                || Objects.isNull(ship.getProdDate())
                || Objects.isNull(ship.getSpeed())
                || Objects.isNull(ship.getCrewSize())
                || ship.getName().length() > 50
                || ship.getPlanet().length() > 50
                || ship.getName().isEmpty()
                || ship.getPlanet().isEmpty()
                || ship.getSpeed() < 0.01
                || ship.getSpeed() > 0.99
                || ship.getCrewSize() < 1
                || ship.getCrewSize() > 9999
                || ship.getProdDate().getTime() < 0
                || !validateProdDate(ship.getProdDate())) throw new Bad_Request_Exception();
    }

    //Проверка даты на корректность
    static boolean validateProdDate(Date date) {
        Date minDate = new Date();
        minDate.setYear(2800 - 1900);

        Date maxDate = new Date();
        maxDate.setYear(3019 - 1900);

        return date.getTime() >= minDate.getTime()
                && date.getTime() <= maxDate.getTime();
    }

    //Расчет рейтинга корабля
    static Double computeShipRating(Ship ship) {
        double ratio;

        /*Если значение поля isUsed равно null или true
        коэффициент равен 0.5, иначе 1.0*/
        if (Objects.nonNull(ship.isUsed()) ? ship.isUsed() : false)
            ratio = 0.5;
        else ratio = 1.0;

        Date date = new Date();
        date.setTime(ship.getProdDate().getTime());

        //Расчитываем точное значение рейтинга
        double exactRating = 80 * ship.getSpeed() * ratio
                / (3019 - date.getYear() - 1900 + 1);

        //Округляем значение рейтинга до сотых
        return Double.valueOf(String.format("%.2f", exactRating).replace(",", "."));
    }
}