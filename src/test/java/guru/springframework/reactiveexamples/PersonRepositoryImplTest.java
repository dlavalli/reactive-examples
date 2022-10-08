package guru.springframework.reactiveexamples;

import guru.springframework.reactiveexamples.domain.Person;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class PersonRepositoryImplTest {

    private PersonRepositoryImpl personRepository;

    @BeforeEach
    void setUp() {
        personRepository = new PersonRepositoryImpl();
    }

    @Test
    void getByIdBlock() {
        Mono<Person> personMono = personRepository.getById(1);
        Person person = personMono.block();  // Block and get instance
        log.info(person.toString());
    }

    @Test
    void getByIdSubscribe() {
        Mono<Person> personMono = personRepository.getById(1);
        StepVerifier.create(personMono).expectNextCount(1).verifyComplete();
        personMono.subscribe(person -> {
            log.info(person.toString());
        });
    }

    @Test
    void getByIdSubscribeNotFound() {
        Mono<Person> personMono = personRepository.getById(9);
        StepVerifier.create(personMono).expectNextCount(0).verifyComplete();
        personMono.subscribe(person -> {
            log.info(person.toString());
        });
    }

    @Test
    void getByIdMapFunction() {
        Mono<Person> personMono = personRepository.getById(1);
        personMono.map(person -> {
            log.info(person.toString()); // Only logs when async call done
            return person.getFirstName();
        }).subscribe(firstName -> {
            log.info("From map firstName: {}", firstName);
        });
    }

    @Test
    void TestFluxBlockFirst() {
        Flux<Person> personFlux = personRepository.findAll();
        Person person = personFlux.blockFirst(); // return 1st element of flux data
        log.info(person.toString());
    }

    @Test
    void testFluxSubscribe() {
        Flux<Person> personFlux = personRepository.findAll();
        StepVerifier.create(personFlux).expectNextCount(4).verifyComplete();
        personFlux.subscribe(person -> {
            log.info(person.toString()); // Called on each instance of the flux data
        });
    }

    @Test
    void TestFluxToListMono() {
        Flux<Person> personFlux = personRepository.findAll();
        Mono<List<Person>> personListMono = personFlux.collectList();
        personListMono.subscribe(list -> {
           list.forEach(person -> {
               log.info(person.toString()); // Called on each instance of the flux data
           });
        });
    }

    @Test
    void testFinPersonById() {
        Flux<Person> personFlux = personRepository.findAll();

        final Integer id = 3;
        Mono<Person> personMono = personFlux.filter(person -> person.getId() == id).next();
        personMono.subscribe(person -> {
           log.info(person.toString());
        });

    }

    @Test
    void testFinPersonByIdNotFound() {  // Fail silently
        Flux<Person> personFlux = personRepository.findAll();

        // In this implementation, the caller does not get anything in the mono due to error
        final Integer id = 8;
        Mono<Person> personMono = personFlux.filter(person -> person.getId() == id).next();
        personMono.subscribe(person -> {
            log.info(person.toString());
        });

    }

    @Test
    void testFinPersonByIdNotFoundWithException() {  // Fail with an exception
        Flux<Person> personFlux = personRepository.findAll();

        final Integer id = 8;
        // In this implementation, the caller receive a default person in the mono due to error
        // Expect and emit a single item from this flux or signal no such element exception or index outofbound if more than one item
        Mono<Person> personMono = personFlux.filter(person -> person.getId() == id).single();
        personMono
                .doOnError(throwable -> log.error("Did not match expected result", throwable))
                .onErrorReturn(Person.builder().id(id).build()) // This is to prevent the exception triggered in the console
                .subscribe(person -> {
            log.info(person.toString());
        });
    }
}