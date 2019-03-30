package com.example.webmvcfn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.servlet.*;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@Log4j2
@SpringBootApplication
public class WebmvcFnApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebmvcFnApplication.class, args);
	}


	@Bean
	RouterFunction<ServerResponse> routes(PersonRepository pr,
																																							PersonHandler ph) {
		return route()
			.GET("/people", ph::handleGetAllPeople)
			.GET("/people/{id}", r -> ok().body(pr.findById(Long.parseLong(r.pathVariable("id")))))
			.POST("/people", req -> {
				var incomingPostedBody = req.body(Person.class);
				var saved = pr.save(incomingPostedBody);
				return ServerResponse.created(URI.create("/people/" + saved.getId())).body(saved);
			})
			.filter((serverRequest, handlerFunction) -> {
				var hff = HandlerFilterFunction.class.getName();
				try {
					log.info("start " + hff);
					return handlerFunction.handle(serverRequest);
				}
				finally {
					log.info("stop " + hff);
				}
			})
			.build();
	}
}

@Component
class PersonHandler {

	private final PersonRepository pr;

	PersonHandler(PersonRepository pr) {
		this.pr = pr;
	}

	ServerResponse handleGetAllPeople(ServerRequest serverRequest) throws Exception {
		return ok().body(pr.findAll());
	}
}

@RestController
class SimpleRestController {

	@GetMapping("/hello")
	String hello() {
		return "hello world";
	}
}

@Log4j2
@Component
class MyServletFilter extends GenericFilter {

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		var gf = GenericFilter.class.getName();
		try {
			log.info("start " + gf);
			filterChain.doFilter(servletRequest, servletResponse);
		}
		finally {
			log.info("stop " + gf);
		}

	}
}

@Log4j2
@Component
class Initializer {

	private final PersonRepository personRepository;

	Initializer(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void start() throws Exception {

		Stream.of("Jane", "Olga", "Madhura", "Kimly", "Tammmie",
			"Violetta", "Cornelia", "Josh")
			.map(name -> new Person(null, name))
			.map(r -> this.personRepository.save(r))
			.forEach(log::info);

	}
}


interface PersonRepository extends JpaRepository<Person, Long> {
}

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
class Person {

	@Id
	@GeneratedValue
	private Long id;

	private String name;
}