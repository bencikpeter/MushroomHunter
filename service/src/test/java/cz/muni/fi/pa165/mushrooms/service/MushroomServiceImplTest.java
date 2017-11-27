package cz.muni.fi.pa165.mushrooms.service;

import cz.muni.fi.pa165.mushrooms.dao.MushroomDao;
import cz.muni.fi.pa165.mushrooms.dao.VisitDao;
import cz.muni.fi.pa165.mushrooms.entity.Mushroom;
import cz.muni.fi.pa165.mushrooms.enums.MushroomType;
import cz.muni.fi.pa165.mushrooms.service.exceptions.EntityFindServiceException;
import cz.muni.fi.pa165.mushrooms.service.exceptions.EntityOperationServiceException;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.muni.fi.pa165.mushrooms.service.TestUtils.validateMushroom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for service layer related to Mushroom entity
 * Not much to test here actually, service is just relaying to DAO layer
 *
 * @author bencikpeter
 */

public class MushroomServiceImplTest {

    @Injectable
    private MushroomDao mushroomDao;

    @Tested(fullyInitialized = true)
    private MushroomServiceImpl service;

    private class MockDatabase {
        private Map<Long, Mushroom> database = new HashMap<>();
        private long databaseCounter = 0;



        public void create(Mushroom mushroom) {
            validateMushroom(mushroom);
            if (mushroom.getId() != null) throw new IllegalArgumentException("already in db");

            mushroom.setId(databaseCounter);
            database.put(databaseCounter++, mushroom);
        }

        public void update(Mushroom mushroom) {
            validateMushroom(mushroom);
            if (mushroom.getId() == null) throw new IllegalArgumentException("not persisted - cannot be updated");
            if (database.replace(mushroom.getId(),mushroom) == null) throw new IllegalArgumentException("no object with such id in DB - cannot be updated");;
        }

        public void delete(Mushroom mushroom){
            validateMushroom(mushroom);
            if (mushroom.getId() == null) throw new IllegalArgumentException("no id assigned");
            database.remove(mushroom.getId());
        }

        public Mushroom findById(Long id) {
            if (id == null){
                throw new IllegalArgumentException("null id");
            }
            return database.get(id);
        }

        public List<Mushroom> findByMushroomType(MushroomType mushroomType){
            List<Mushroom> typedList = new ArrayList<>();
            for (Mushroom m : database.values()) {
                if (m.getType().equals(mushroomType)) typedList.add(m);
            }
            return typedList;
        }

        public List<Mushroom> findAll(){
            return Collections.unmodifiableList(new ArrayList<>(database.values()));
        }

        public List<Mushroom> findByIntervalOfOccurrence(String fromMonth, String toMonth){
            String intervalOfOccurrence = fromMonth + " - " + toMonth;
            List<Mushroom> typedList = new ArrayList<>();
            for (Mushroom m : database.values()) {
                if (m.getIntervalOfOccurrence().equals(intervalOfOccurrence)) typedList.add(m);
            }

            return typedList;
        }

        public Mushroom findByName(String name) {
            if (name == null) throw new IllegalArgumentException("Name is null");
            List<Mushroom> typedList = new ArrayList<>();
            List<Mushroom> dumpedDatabase = Collections.unmodifiableList(new ArrayList<>(database.values()));
            for (Mushroom m : dumpedDatabase) {
                if (m.getName().equals(name)) return m;
            }
            return null;
        }

    }

    MockDatabase database;
    Mushroom mushroom1, mushroom2, mushroom3;

    private static Mushroom setupMushroom(String name, MushroomType type, String fromM, String toM){
        Mushroom m = new Mushroom();
        m.setName(name);
        m.setIntervalOfOccurrence(fromM,toM);
        m.setType(type);
        return m;
    }

    @Before
    public void setUp(){
        //TODO: actual setup

        database = new MockDatabase();
        mushroom1 = setupMushroom("some",MushroomType.UNEDIBLE,"june","july");
        mushroom2 = setupMushroom("other",MushroomType.POISONOUS,"june","july");
        mushroom3 = setupMushroom("different",MushroomType.UNEDIBLE,"may","september");



        new Expectations(){{
            mushroomDao.findById(anyLong);
            result = new Delegate() {
                Mushroom foo(Long id){
                    return database.findById(id);
                }
            }; minTimes = 0;

            mushroomDao.findByMushroomType((MushroomType) any);
            result = new Delegate() {
                List<Mushroom> foo(MushroomType type){
                    return database.findByMushroomType(type);
                }
            }; minTimes = 0;

            mushroomDao.create((Mushroom) any);
            result = new Delegate() {
                void foo(Mushroom m){
                    database.create(m);
                }
            }; minTimes = 0;

            mushroomDao.update((Mushroom) any);
            result = new Delegate() {
                void foo(Mushroom m){
                    database.update(m);
                }
            }; minTimes = 0;

            mushroomDao.delete((Mushroom) any);
            result = new Delegate() {
                void foo(Mushroom m){
                    database.delete(m);
                }
            }; minTimes = 0;

            mushroomDao.findAll();
            result = new Delegate() {
                List<Mushroom> foo(){
                    return database.findAll();
                }
            }; minTimes = 0;

            mushroomDao.findByName(anyString);
            result = new Delegate() {
                Mushroom foo(String name){
                    return database.findByName(name);
                }
            }; minTimes = 0;

            mushroomDao.findByIntervalOfOccurrence(anyString, anyString);
            result = new Delegate() {
                List<Mushroom> foo(String fromMonth, String toMonth){
                    return database.findByIntervalOfOccurrence(fromMonth,toMonth);
                }
            }; minTimes = 0;
        }};
    }

    @Test
    public void findAllMushrooms(){

        assertThat(service.findAllMushrooms()).isEmpty();

        database.create(mushroom1);
        database.create(mushroom2);
        database.create(mushroom3);

        assertThat(service.findAllMushrooms()).containsExactlyInAnyOrder(mushroom1,mushroom2,mushroom3);
    }

    @Test
    public void findMushroomById(){
        database.create(mushroom1);
        database.create(mushroom2);
        //null
        assertThatThrownBy(() -> service.findMushroomById(null)).isInstanceOf(DataAccessException.class);
        //valid
        assertThat(service.findMushroomById(mushroom1.getId())).isEqualToComparingFieldByField(mushroom1);
        //invalid
        assertThat(service.findMushroomById(999L)).isNull();
    }

    @Test
    public void FindMushroomByName(){

        database.create(mushroom1);
        database.create(mushroom2);

        //null name
        assertThatThrownBy(() -> service.findMushroomByName(null)).isInstanceOf(DataAccessException.class);
        //nonexistent name
        assertThat(service.findMushroomByName(mushroom3.getName())).isNull();
        //existing name
        database.create(mushroom3);
        assertThat(service.findMushroomByName(mushroom3.getName())).isEqualToComparingFieldByField(mushroom3);
        //empty string
        assertThat(service.findMushroomByName("")).isNull();

    }

    @Test
    public void findByIntervalOfOccurrence(){}

    @Test
    public void createMushroom(){

        assertThat(mushroom1.getId()).isNull();
        service.createMushroom(mushroom1);
        assertThat(mushroom1.getId()).isNotNull();


        //entity with preexisting ID
        mushroom2.setId(2L);
        assertThatThrownBy(()->service.createMushroom(mushroom2)).isInstanceOf(DataAccessException.class);

        //entity with conflicting ID
        mushroom2.setId(mushroom1.getId());
        assertThatThrownBy(()->service.createMushroom(mushroom2)).isInstanceOf(DataAccessException.class);

        //null
        assertThatThrownBy(()->service.createMushroom(null)).isInstanceOf(DataAccessException.class);
    }

    @Test
    public void deleteMushroom(){

        database.create(mushroom1);

        //delete nonexistent without ID
        assertThatThrownBy(()->service.deleteMushroom(mushroom2)).isInstanceOf(DataAccessException.class);
        //delete nonexistent with ID
        mushroom2.setId(2L);
        service.deleteMushroom(mushroom2);
        assertThat(database.findAll()).containsExactlyInAnyOrder(mushroom1); //checks that invalid delete does not modify database
        //correct delete
        service.deleteMushroom(mushroom1);
        assertThat(database.findAll()).isEmpty();
        //delete null
        assertThatThrownBy(()->service.deleteMushroom(null)).isInstanceOf(DataAccessException.class);
    }

    @Test
    public void updateMushroom(){

        database.create(mushroom1);
        database.create(mushroom3);

        //update no id
        assertThatThrownBy(()->service.updateMushroom(mushroom2)).isInstanceOf(DataAccessException.class);
        //update nonexistent ID
        mushroom2.setId(1234L);
        assertThatThrownBy(()->service.updateMushroom(mushroom2)).isInstanceOf(DataAccessException.class);
        //correct update
        String newName = "Totaly new name";
        mushroom1.setName(newName);
        service.updateMushroom(mushroom1);
        Mushroom tmpMush = database.findById(mushroom1.getId());
        assertThat(tmpMush.getName().equals(newName));
        //update null
        assertThatThrownBy(()->service.updateMushroom(null)).isInstanceOf(DataAccessException.class);
    }

}
