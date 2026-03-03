package com.banking.system.account.infraestructure.adapter.out.generator;

import com.banking.system.account.domain.model.value_object.AccountAlias;
import com.banking.system.account.domain.port.out.AccountAliasGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

/**
 * Infrastructure adapter that generates random, human-readable account aliases.
 * Format: {adjective}.{noun}.{2-3 digits}
 */
@Component
public class AccountAliasGeneratorAdapter implements AccountAliasGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final List<String> ADJECTIVES = List.of(
            "happy", "blue", "quick", "bright", "calm", "clever", "cool", "cute",
            "eager", "fair", "fancy", "fine", "gentle", "glad", "good", "grand",
            "jolly", "kind", "light", "lively", "lucky", "merry", "nice", "proud",
            "quiet", "rapid", "rich", "safe", "sharp", "smooth", "soft", "solid",
            "sweet", "tall", "warm", "wild", "wise", "young", "zealous", "brave"
    );

    private static final List<String> NOUNS = List.of(
            "tree", "sky", "moon", "star", "sun", "cloud", "river", "lake",
            "hill", "rock", "bird", "fish", "wolf", "bear", "deer", "fox",
            "lion", "tiger", "eagle", "hawk", "storm", "wind", "rain", "snow",
            "fire", "water", "earth", "stone", "wave", "peak", "dawn", "dusk",
            "ocean", "forest", "valley", "canyon", "island", "beach", "reef", "shore"
    );

    @Override
    public AccountAlias generate() {
        String adjective = ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(RANDOM.nextInt(NOUNS.size()));
        int number = RANDOM.nextInt(900) + 100; // 100-999 (3 digits)

        String aliasValue = String.format("%s.%s.%d", adjective, noun, number);
        return new AccountAlias(aliasValue);
    }
}
