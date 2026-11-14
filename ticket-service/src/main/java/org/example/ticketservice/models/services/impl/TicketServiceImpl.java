package org.example.ticketservice.models.services.impl;

import jakarta.persistence.criteria.Order;
import lombok.RequiredArgsConstructor;
import org.example.ticketservice.models.constants.TicketStatus;
import org.example.ticketservice.models.dto.requests.CreateTicketDetailRequest;
import org.example.ticketservice.models.dto.requests.CreateTicketRequest;
import org.example.ticketservice.models.dto.responses.TicketDetailResponse;
import org.example.ticketservice.models.dto.responses.TicketResponse;
import org.example.ticketservice.models.dto.responses.TripResponse;
import org.example.ticketservice.models.entities.Ticket;
import org.example.ticketservice.models.entities.TicketDetail;
import org.example.ticketservice.models.repositories.TicketDetailRepository;
import org.example.ticketservice.models.repositories.TicketRepository;
import org.example.ticketservice.models.services.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private static final String ORDER_CREATED_TOPIC = "order-created";

    private final TicketRepository ticketRepository;
    private final TicketDetailRepository ticketDetailRepository;
    private final TripGatewayService tripGatewayService;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        List<TicketDetail> itemHolders = new ArrayList<>();
        double total = 0.0;
        for (CreateTicketDetailRequest itemReq : request.items()) {
            TripResponse trip = tripGatewayService.getTripById(itemReq.tripId());
            if (itemReq.seat() <= 0 || itemReq.seat() > trip.availableSeats()) {
                throw new IllegalArgumentException("Invalid seat count for trip ID: " + itemReq.tripId());
            }
            double lineTotal = trip.ticketPrice() * itemReq.seat();
            total += lineTotal;

            TicketDetail ticketDetail = TicketDetail.builder()
                    .tripId(itemReq.tripId())
                    .seats(itemReq.seat())
                    .ticketPrice(trip.ticketPrice())
                    .lineTotal(lineTotal)
                    .build();
            itemHolders.add(ticketDetail);
        }

        Ticket ticket = Ticket.builder()
                .passengerName(request.passengerName())
                .totalAmount(total)
                .status(TicketStatus.PENDING)
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);

        List<TicketDetailResponse> ticketDetailResponses = new ArrayList<>();
        for (TicketDetail ticketDetail : itemHolders) {
            ticketDetail.setTicket(savedTicket);
            TicketDetail savedDetail = ticketDetailRepository.save(ticketDetail);
            ticketDetailResponses.add(new TicketDetailResponse(
                    savedDetail.getId(),
                    savedDetail.getTripId(),
                    savedDetail.getSeats(),
                    savedDetail.getTicketPrice(),
                    savedDetail.getLineTotal()
            ));
        }

        return new TicketResponse(savedTicket.getId(), savedTicket.getPassengerName(), savedTicket.getPassengerEmail(), savedTicket.getTotalAmount(), savedTicket.getStatus(), ticketDetailResponses);

    }
}


