package hei.school.graduation.endpoint.event.consumer.model;

import hei.school.graduation.PojaGenerated;
import hei.school.graduation.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
