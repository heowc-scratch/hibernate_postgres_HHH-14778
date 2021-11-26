package com.example;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@NamedQuery(
        name = "fixedCount",
        query = "UPDATE Message m SET m.count = :count WHERE m.id = :id"
)
public class Message {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long count;

    private String body;

    @Builder
    protected Message(Long count, String body) {
        this.count = count;
        this.body = body;
    }
}
