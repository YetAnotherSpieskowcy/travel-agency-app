package pl.edu.pg.rsww.api_gateway

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NotFoundException : RuntimeException {
    constructor() : super("Not Found")
}
