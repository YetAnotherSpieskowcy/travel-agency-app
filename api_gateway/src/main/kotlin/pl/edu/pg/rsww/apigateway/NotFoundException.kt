package pl.edu.pg.rsww.apigateway

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NotFoundException : RuntimeException {
    constructor() : super("Strony nie znaleziono")
}
