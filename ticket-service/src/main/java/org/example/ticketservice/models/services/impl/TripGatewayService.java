package org.example.ticketservice.models.services.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.example.ticketservice.clients.TripClient;
import org.example.ticketservice.exceptions.TripNotFoundException;
import org.example.ticketservice.exceptions.TripServiceException;
import org.example.ticketservice.models.dto.responses.TripResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TripGatewayService {
    private final TripClient tripClient;

    public TripResponse getTripById(Long tripId) {
        try {
            TripResponse trip = tripClient.getTripById(tripId);
            if (trip == null) {
                throw new TripNotFoundException(tripId);
            }
            return trip;
        } catch (FeignException.NotFound e) {
            throw new TripNotFoundException(tripId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new TripNotFoundException(tripId);
            }
            throw new TripServiceException("Trip service call failed", e);
        } catch (TripNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new TripServiceException("Trip service call failed", e);
        }
    }
    public TripResponse getTripByIdFallback(Long tripId, Throwable throwable) {
        if (throwable instanceof TripNotFoundException) {
            throw (TripNotFoundException) throwable;
        }
        if (throwable instanceof TripServiceException) {
            throw (TripServiceException) throwable;
        }
        throw new TripServiceException("Trip service is currently unavailable", throwable);
    }

}

