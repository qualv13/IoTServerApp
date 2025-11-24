package org.qualv13.iotbackend.service;

import org.qualv13.iotbackend.entity.Lamp;
import org.qualv13.iotbackend.entity.User;
import org.qualv13.iotbackend.repository.LampRepository;
import org.qualv13.iotbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LampService {
    private final LampRepository lampRepository;
    private final UserRepository userRepository;

    @Transactional
    public void assignLampToUser(String username, String lampId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Lamp lamp = lampRepository.findById(lampId).orElse(new Lamp());

        // LOGIKA BIZNESOWA:
        // Jeśli lampa miała właściciela, nadpisujemy go (re-selling).
        // dodać logowanie zdarzenia, np. "Ownership transfer from X to Y"

        lamp.setId(lampId);
        lamp.setOwner(user);
        lamp.setFleet(null); // Reset floty przy zmianie właściciela

        lampRepository.save(lamp);
    }
}
