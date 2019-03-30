package com.example.webmvcfn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.servlet.*;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@Log4j2
@SpringBootApplication
public class WebmvcFnApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebmvcFnApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes(PersonHandler ph) {
		var root = "";
		return route()
			.GET(root + "/people", ph::handleGetAllPeople)
			.GET(root + "/people/{id}", ph::handleGetPersonById)
			.POST(root + "/people", ph::handlePostPerson)
			.filter((serverRequest, handlerFunction) -> {
				try {
					log.info("entering HandlerFilterFunction");
					return handlerFunction.handle(serverRequest);
				}
				finally {
					log.info("exiting HandlerFilterFunction");
				}
			})
			.build();
	}
}

@Log4j2
@Component
class SimpleFilter extends GenericFilter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
																						FilterChain filterChain) throws IOException, ServletException {
		log.info("entering SimpleFilter");
		filterChain.doFilter(req, res);
		log.info("exiting SimpleFilter");
	}
}

@Component
class PersonHandler {

	private final PersonService personService;

	PersonHandler(PersonService personService) {
		this.personService = personService;
	}

	ServerResponse handleGetAllPeople(ServerRequest serverRequest) {
		return ok().body(personService.all());
	}

	ServerResponse handlePostPerson(ServerRequest r) throws ServletException, IOException {
		var result = personService.save(new Person(null, r.body(Person.class).getName()));
		var uri = URI.create("/people/" + result.getId());
		return ServerResponse.created(uri).body(result);
	}

	ServerResponse handleGetPersonById(ServerRequest r) {
		return ok().body(personService.byId(Long.parseLong(r.pathVariable("id"))));
	}
}

@RestController
class GreetingsRestController {

	@GetMapping("/greet/{name}")
	String greet(@PathVariable String name) {
		return "hello " + name + "!";
	}
}

@Service
class PersonService {

	private final AtomicLong counter = new AtomicLong();

	private final Set<Person> people = new HashSet<>(Set.of(new Person(counter.incrementAndGet(), "Jane"),
		new Person(counter.incrementAndGet(), "Josh"), new Person(counter.incrementAndGet(), "Gordon"), new Person(counter.incrementAndGet(), "Tammie")));

	Person save(Person p) {
		var person = new Person(counter.incrementAndGet(), p.getName());
		this.people.add(person);
		return person;
	}

	Set<Person> all() {
		return this.people;
	}

	Person byId(Long id) {
		return this.people.stream()
			.filter(p -> p.getId().equals(id))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("no " + Person.class.getName() + " with that ID found!"));
	}

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Person {
	private Long id;
	private String name;
}