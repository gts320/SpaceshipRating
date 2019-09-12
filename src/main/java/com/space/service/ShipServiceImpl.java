package com.space.service;

import com.space.controller.ShipOrder;
import com.space.exceptions.Bad_Request_Exception;
import com.space.exceptions.NotFound_Exception;
import com.space.model.Ship;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ShipServiceImpl implements ShipService {
    private final ShipRepository shipRepository;

    @Autowired
    public ShipServiceImpl(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    @Override
    @Transactional
    public List<Ship> getShipsList(String name,
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
                                   Double maxRating,
                                   ShipOrder order,
                                   Integer pageNumber,
                                   Integer pageSize) {

        /*Если @param order не равен null, получаем сортированный список по @param order,
          иначе получаем несортированный список*/
        List<Ship> shipsToFiltered = order != null ?
                shipRepository.findAll(Sort.by(order.getFieldName(), "id"))
                : shipRepository.findAll();

        //Получаем фильтрованный список
        List<Ship> filteredShips = ShipsUtils.filterShips(
                shipsToFiltered, name, planet, shipType, after, before, isUsed, minSpeed,
                maxSpeed, minCrewSize, maxCrewSize, minRating, maxRating);

        /*Если @param pageNumber и @param pageSize равны null
        устанавливаем для них значения по умолчанию*/
        pageNumber = pageNumber == null ? 0 : pageNumber;
        pageSize = pageSize == null ? 3 : pageSize;

        /*Формируем подсписок кораблей, расположенных на странице @param pageNumber*/
        List<Ship> sublistByPageNumber = filteredShips.subList(pageNumber * pageSize, filteredShips.size());

        /*Формируем подсписок кораблей, ограничивая подсписок sublistByPageNumber
        до размера @param pageSize*/
        return sublistByPageNumber.subList(0, Math.min(pageSize, sublistByPageNumber.size()));
    }

    @Override
    @Transactional
    public Integer getShipsCount(String name,
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

        //Получаем фильтрованный список
        return ShipsUtils.filterShips(
                shipRepository.findAll(), name, planet, shipType, after, before, isUsed,
                minSpeed, maxSpeed, minCrewSize, maxCrewSize, minRating, maxRating).size();
    }

    @Override
    @Transactional
    public Ship createShip(Ship requestBody) {
        //Если тело запроса пустое, новый корабль не создается
        if (Objects.isNull(requestBody)) return null;

        //Проверяем на корректность параметры создаваемого корабля
        ShipsUtils.validateParameters(requestBody);

        //Создаем новый корабль
        Ship creatingShip = new Ship();

        //Конструируем новый корабль
        creatingShip.setName(requestBody.getName());
        creatingShip.setPlanet(requestBody.getPlanet());
        creatingShip.setShipType(requestBody.getShipType());
        creatingShip.setProdDate(requestBody.getProdDate());
        creatingShip.setUsed(Objects.nonNull(requestBody.isUsed()) ? requestBody.isUsed() : false);
        creatingShip.setSpeed(requestBody.getSpeed());
        creatingShip.setCrewSize(requestBody.getCrewSize());
        creatingShip.setRating(ShipsUtils.computeShipRating(requestBody));

        //Сохраняем созданный корабль в БД
        shipRepository.save(creatingShip);

        return creatingShip;
    }

    @Override
    @Transactional
    public Ship getShip(Long id) {
        //Если значение ID корабля некорректно, отвечаем ошибкой с кодом 400
        if (id <= 0) throw new Bad_Request_Exception();

        //Если корабль не найден в БД, отвечаем ошибкой с кодом 404
        return shipRepository.findById(id).orElseThrow(NotFound_Exception::new);
    }

    /*Меняем значения для всех полей обновляемого корабля,
    предварительно проверяя на null соответствующие значения полей из тела запроса.
    Если они равны null, пропускаем их
    На некорректные значения полей из тела запроса отвечаем ошибкой с кодом 400*/
    @Override
    @Transactional
    public Ship updateShip(Long id, Ship requestBody) {
        //Получаем корабль из БД по ID
        Ship updatingShip = getShip(id);

        if (Objects.nonNull(requestBody.getName()))
            if (!requestBody.getName().isEmpty()
                    && requestBody.getName().length() <= 50)
                updatingShip.setName(requestBody.getName());
            else throw new Bad_Request_Exception();

        if (Objects.nonNull(requestBody.getPlanet()))
            if (requestBody.getPlanet().length() <= 50)
                updatingShip.setPlanet(requestBody.getPlanet());
            else throw new Bad_Request_Exception();

        if (Objects.nonNull(requestBody.getShipType()))
            updatingShip.setShipType(requestBody.getShipType());

        if (Objects.nonNull(requestBody.getProdDate())) {
            if (requestBody.getProdDate().getTime() > 0
                    && ShipsUtils.validateProdDate(requestBody.getProdDate()))
                updatingShip.setProdDate(requestBody.getProdDate());
            else throw new Bad_Request_Exception();
        }

        if (Objects.nonNull(requestBody.isUsed()))
            updatingShip.setUsed(requestBody.isUsed());

        if (Objects.nonNull(requestBody.getSpeed()))
            if (requestBody.getSpeed() >= 0.01 || requestBody.getSpeed() <= 0.99)
                updatingShip.setSpeed(requestBody.getSpeed());
            else throw new Bad_Request_Exception();

        if (Objects.nonNull(requestBody.getCrewSize()))
            if (requestBody.getCrewSize() >= 1 && requestBody.getCrewSize() <= 9999)
                updatingShip.setCrewSize(requestBody.getCrewSize());
            else throw new Bad_Request_Exception();

        //Рассчитываем рейтинг корабля
        Double rating = ShipsUtils.computeShipRating(updatingShip);

        updatingShip.setRating(rating);
        //Сохраняем обновленный корабль в БД
        shipRepository.save(updatingShip);

        return updatingShip;
    }

    @Override
    @Transactional
    public void deleteShip(Long id) {
        //Удаляем полученный по ID корабль из БД
        shipRepository.delete(getShip(id));
    }
}
