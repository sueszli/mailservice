package dslab.model;

import dslab.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class InboxEmail extends Email {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
