package org.qualv13.iotbackend.service;

import org.qualv13.iotbackend.dto.LampDto;
import org.qualv13.iotbackend.entity.Fleet;
import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.FleetRepository;
import org.qualv13.iotbackend.repository.LampRepository;
import lombok.RequiredArgsConstructor;
import org.qualv13.iotbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FleetService {
    private final FleetRepository fleetRepository;
    private final LampRepository lampRepository;
    private final UserRepository userRepository;

    public List<LampDto> getLampsInFleet(Long fleetId) {
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new RuntimeException("Fleet not found"));

        return fleet.getLamps().stream()
                .map(l -> new LampDto(l.getId(), l.isOn(), l.getDeviceName(), l.isOnline(), l.getFleet().getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addLampToFleet(Long fleetId, String lampId) {
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new RuntimeException("Fleet not found"));
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        lamp.setFleet(fleet);
        lampRepository.save(lamp);
    }

    @Transactional
    public void removeLampFromFleet(Long fleetId, String lampId) {
        Lamp lamp = lampRepository.findById(lampId)
                .orElseThrow(() -> new RuntimeException("Lamp not found"));

        if (lamp.getFleet() != null && lamp.getFleet().getId().equals(fleetId)) {
            lamp.setFleet(null);
            lampRepository.save(lamp);
        }
    }

    public List<Fleet> getMyFleets(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        // Zakładamy, że masz metodę w repozytorium lub filtrujesz (dla uproszczenia tutaj stream):
        // Lepiej dodać w FleetRepository: List<Fleet> findByOwnerUsername(String username);
        return fleetRepository.findAll().stream()
                .filter(f -> f.getOwner() != null && f.getOwner().getUsername().equals(username))
                .collect(Collectors.toList());
    }

    @Transactional
    public void createFleet(String name, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Fleet fleet = new Fleet();
        fleet.setName(name);
        fleet.setOwner(user);
        fleetRepository.save(fleet);
    }
}