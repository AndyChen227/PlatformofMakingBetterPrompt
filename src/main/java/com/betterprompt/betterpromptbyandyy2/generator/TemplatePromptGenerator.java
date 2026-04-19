package com.betterprompt.betterpromptbyandyy2.generator;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Template-based prompt generator.
 *
 * Covers 5 task types × 3 verbosity levels × 3 variants each = 45 templates.
 *
 * Task types : CODING | EXPLAIN | DEBUG | WRITING | COMPARE
 * Verbosity  : LOW    | MEDIUM  | HIGH
 *
 * LOW    — nearly clean prompts; minimal filler (an occasional "please" / "could you")
 * MEDIUM — noticeable opener greeting + closing line + a couple of verbose phrases
 * HIGH   — stacked salutations, repeated politeness, closing filler, maximally "dirty"
 *
 * Calling generate() picks one variant at random so repeated calls return variety.
 */
@Component
public class TemplatePromptGenerator {

    /**
     * Outer key: "TASKTYPE_VERBOSITY"  (e.g. "CODING_HIGH")
     * Value    : list of prompt strings to pick from randomly
     */
    private static final Map<String, List<String>> TEMPLATES = new HashMap<>();

    static {

        // ══════════════════════════════════════════════════════════════
        // CODING · LOW
        // ══════════════════════════════════════════════════════════════
        put("CODING_LOW",
            "Write a Python function that sorts a list of integers in ascending order.",

            "Could you write a JavaScript function that validates an email address using a regex?",

            "Please write a SQL query to retrieve the top 5 customers ranked by total order value."
        );

        // ══════════════════════════════════════════════════════════════
        // CODING · MEDIUM
        // ══════════════════════════════════════════════════════════════
        put("CODING_MEDIUM",
            "Hi! I'd like you to write a Python function that reverses a string. " +
            "It would be great if you could include comments explaining what the code does. Thanks in advance!",

            "Hello! Could you help me write a Java class that implements a simple stack data structure? " +
            "Please make sure it includes push, pop, and peek methods. Let me know if you need any clarification.",

            "Hey there! I was hoping you could write a TypeScript function to fetch data from " +
            "a REST API and handle errors gracefully. I'd appreciate it if you could keep the code " +
            "clean and well-structured. Thanks a lot!"
        );

        // ══════════════════════════════════════════════════════════════
        // CODING · HIGH
        // ══════════════════════════════════════════════════════════════
        put("CODING_HIGH",
            "Hello there! I hope you're doing absolutely wonderfully today! I was wondering if you " +
            "could possibly help me out with something. I need you to write a function in Python that " +
            "sorts a list of numbers. Could you please make sure it's easy to understand and " +
            "well-commented? I would really appreciate it if you could do that for me. " +
            "Thanks so much in advance, and please let me know if you need any more information from me!",

            "Hi there! I hope you're having a fantastic day! I just wanted to reach out and ask if " +
            "you could potentially help me with a small coding task. I was thinking it would be really " +
            "wonderful if you could write a JavaScript function — and I know this might be a lot to ask " +
            "— that checks whether a given string is a palindrome. Could you please make sure it's easy " +
            "to read and has comments explaining each step? I would be so grateful if you could do this " +
            "for me, and please don't hesitate to let me know if you need any additional context. " +
            "Thanks a million, I truly appreciate your help!",

            "Good day! I sincerely hope you are doing absolutely wonderfully on this fine day. I am " +
            "writing to humbly request your assistance with a coding matter I have been contemplating. " +
            "If it is not too much trouble, I would be eternally grateful if you could possibly help me " +
            "write a Python class that implements a basic linked list with insert and delete operations. " +
            "I understand you are very busy and I am very sorry to trouble you, but I truly believe " +
            "you are the best person to help me with this. Please feel free to ask if anything is " +
            "unclear. Thank you ever so much in advance for your time and consideration, and please " +
            "let me know if you have any questions!"
        );

        // ══════════════════════════════════════════════════════════════
        // EXPLAIN · LOW
        // ══════════════════════════════════════════════════════════════
        put("EXPLAIN_LOW",
            "Explain how binary search works, including its time complexity.",

            "Could you explain what a REST API is and how it differs from a SOAP API?",

            "Please explain the difference between TCP and UDP, with a focus on use cases."
        );

        // ══════════════════════════════════════════════════════════════
        // EXPLAIN · MEDIUM
        // ══════════════════════════════════════════════════════════════
        put("EXPLAIN_MEDIUM",
            "Hi! Could you explain how garbage collection works in Java? " +
            "It would be helpful if you could include a simple example to illustrate the concept. Thanks!",

            "Hello! I'd appreciate it if you could explain what Docker containers are and how they " +
            "differ from virtual machines. Please keep it reasonably concise and clear.",

            "Hey! Could you explain how OAuth 2.0 authentication works? " +
            "A step-by-step breakdown would be great. Let me know if you need me to clarify anything!"
        );

        // ══════════════════════════════════════════════════════════════
        // EXPLAIN · HIGH
        // ══════════════════════════════════════════════════════════════
        put("EXPLAIN_HIGH",
            "Hello! I hope you're having a great day! I have been trying to understand something for " +
            "quite a while now, and I was hoping you might be able to help me out. Could you possibly " +
            "explain how neural networks work? I know it's a big topic and I really don't want to take " +
            "up too much of your time, but I would be so grateful if you could break it down in a way " +
            "that's easy to understand. Feel free to use any examples you think might help! I really " +
            "appreciate your time and effort, and I hope this isn't too much to ask. Thank you so much!",

            "Hi there! I sincerely hope you are doing extremely well today. I have been spending some " +
            "time trying to wrap my head around recursion in programming, and I must admit I'm finding " +
            "it quite difficult. I was wondering if you could possibly be so kind as to explain it to " +
            "me? If you could provide an example or two, that would be absolutely wonderful. I would " +
            "really appreciate your patience and help with this, and please do let me know if you have " +
            "any questions about what I'm asking. Thank you so much in advance for your time!",

            "Good day to you! I truly hope everything is going wonderfully for you today and always. " +
            "I wanted to reach out and ask if you might possibly have a moment to help me understand " +
            "something I've been struggling with for a while. I've been trying to learn about " +
            "polymorphism in object-oriented programming and I keep getting confused. Could you please, " +
            "if it's not too much trouble, explain what it is and how it works? I would be so thrilled " +
            "if you could include some real-world examples to make it clearer. I am very thankful for " +
            "any help you can give, and please don't hesitate to let me know if you need more context. " +
            "Thanks a million!"
        );

        // ══════════════════════════════════════════════════════════════
        // DEBUG · LOW
        // ══════════════════════════════════════════════════════════════
        put("DEBUG_LOW",
            "This Python code raises an IndexError. Find the bug:\n" +
            "my_list = [1, 2, 3]\nprint(my_list[5])",

            "Could you identify why this JavaScript function returns undefined:\n" +
            "function add(a, b) { return; a + b; }",

            "Please find the issue in this SQL query that returns no rows:\n" +
            "SELECT * FROM users WHERE id = '5'  -- id column is INTEGER"
        );

        // ══════════════════════════════════════════════════════════════
        // DEBUG · MEDIUM
        // ══════════════════════════════════════════════════════════════
        put("DEBUG_MEDIUM",
            "Hi! I have a bug in my Python code and I'm not sure what's causing it. The function is " +
            "supposed to calculate the factorial but it seems to run forever. Could you help me " +
            "identify what's going wrong? Here's the code:\n\ndef fact(n):\n    return n * fact(n - 1)\n\nThanks!",

            "Hello! I've been struggling with this JavaScript bug for a while. My async function seems " +
            "to return a Promise object instead of the resolved value. Could you help me figure out " +
            "what's wrong and how to fix it? I'd appreciate a clear explanation.",

            "Hey, I'm getting a NullPointerException in my Java code but I can't figure out why. " +
            "The error happens when I call .length() on a string that I thought was initialised. " +
            "Could you help me debug this and explain the likely root cause? Thanks!"
        );

        // ══════════════════════════════════════════════════════════════
        // DEBUG · HIGH
        // ══════════════════════════════════════════════════════════════
        put("DEBUG_HIGH",
            "Hello! I hope you're having a wonderful day! I have been struggling with a really " +
            "frustrating bug in my code for what feels like forever, and I am honestly at my wit's " +
            "end. I was hoping you might be able to help me figure out what's going wrong. My Python " +
            "script keeps throwing a KeyError exception and I have absolutely no idea why, even though " +
            "I've read the documentation multiple times. Could you please, if you have the time, help " +
            "me understand what might be causing this? I would be incredibly grateful for any insight. " +
            "Please let me know if you need to see more of my code. Thanks so much in advance!",

            "Hi there! I sincerely hope you're doing absolutely great today! I am reaching out because " +
            "I have been dealing with this incredibly puzzling bug for several days now and I just " +
            "cannot seem to figure it out no matter what I try. My JavaScript code is supposed to " +
            "update the DOM but nothing seems to change on the page. I know you must be very busy and " +
            "I completely understand if this is a lot to ask, but I would be so deeply grateful if " +
            "you could take a look. I've tried literally everything I can think of! Please feel free " +
            "to ask me anything you need. Thank you so very much for your time and help!",

            "Good day! I truly hope things are going wonderfully for you today and always. I am " +
            "writing with a request I hope you will not find too burdensome. I have been encountering " +
            "a most perplexing StackOverflowError in my Java application, and despite my best efforts " +
            "and extensive research I have been unable to resolve it. I understand this may be complex " +
            "and I am very sorry to trouble you with it, but I would be forever grateful if you could " +
            "help me identify the root cause. Any guidance you can offer would be immensely valuable. " +
            "Please do not hesitate to ask for any additional information. Thank you from the bottom " +
            "of my heart for your time and patience!"
        );

        // ══════════════════════════════════════════════════════════════
        // WRITING · LOW
        // ══════════════════════════════════════════════════════════════
        put("WRITING_LOW",
            "Write a brief technical blog post introduction about microservices architecture.",

            "Could you write a short README description for a web scraping tool built with Python?",

            "Please write a concise commit message for a change that adds input validation to a login form."
        );

        // ══════════════════════════════════════════════════════════════
        // WRITING · MEDIUM
        // ══════════════════════════════════════════════════════════════
        put("WRITING_MEDIUM",
            "Hi! Could you help me write a professional email to my team announcing a new deployment " +
            "process? It should be friendly but informative and cover the key steps involved. Thanks in advance!",

            "Hello! I need to write a short technical summary of my machine learning project for a " +
            "portfolio. Could you help me draft something that highlights the key technologies and " +
            "outcomes? I'd really appreciate your help!",

            "Hey! Could you help me write a LinkedIn post announcing that I've just completed a full-stack " +
            "React course? It should sound professional but genuinely enthusiastic. Thanks!"
        );

        // ══════════════════════════════════════════════════════════════
        // WRITING · HIGH
        // ══════════════════════════════════════════════════════════════
        put("WRITING_HIGH",
            "Hello there! I hope you are having the most wonderful day! I have been putting off " +
            "writing something important for quite a while now and I thought maybe you could help me. " +
            "I need to write a cover letter for a software engineering internship, and writing has " +
            "never really been my strongest suit, to be completely honest. Could you please help me " +
            "craft a letter that's professional, engaging, and highlights my technical skills well? " +
            "I know this is a lot to ask and I am very sorry to trouble you with it. I would be so " +
            "incredibly grateful for your help and expertise. Please let me know if you need any " +
            "information about my background! Thank you so, so much in advance!",

            "Hi! I hope everything is going amazing for you today! I've been trying to write a " +
            "technical blog post about Docker for beginners for quite some time now, but I keep " +
            "struggling to find the right words and structure. I was wondering if you could possibly " +
            "help me write an engaging introduction that's accessible for people who've never used " +
            "Docker? I know you're probably very busy and I genuinely appreciate you taking the time " +
            "to read this long message. I would be so, so thankful if you could help me out! Please " +
            "don't hesitate to let me know if you need more details. Thanks a million!",

            "Good day! I sincerely hope this message finds you in the best of spirits today! I am " +
            "reaching out with a humble request for your invaluable assistance. I have been tasked " +
            "with writing a project proposal document for a new mobile application, and I am honestly " +
            "feeling quite overwhelmed by the scope of it. I was wondering if you could possibly help " +
            "me write an executive summary that clearly and compellingly articulates the project's " +
            "value proposition. I know this is asking a great deal, and I am truly so grateful for " +
            "any help you can provide. Please do not hesitate to ask if you have any questions at all. " +
            "Thank you ever so much, I really do deeply appreciate your time and effort!"
        );

        // ══════════════════════════════════════════════════════════════
        // COMPARE · LOW
        // ══════════════════════════════════════════════════════════════
        put("COMPARE_LOW",
            "Compare Python and JavaScript for backend development, covering performance and ecosystem.",

            "Could you compare MySQL and PostgreSQL for a high-traffic web application?",

            "Please compare REST and GraphQL APIs in terms of flexibility, performance, and developer experience."
        );

        // ══════════════════════════════════════════════════════════════
        // COMPARE · MEDIUM
        // ══════════════════════════════════════════════════════════════
        put("COMPARE_MEDIUM",
            "Hi! Could you compare React and Vue.js for building a medium-sized single-page application? " +
            "I'd like to understand the trade-offs in learning curve, ecosystem, and performance. Thanks!",

            "Hello! I'm trying to decide between MongoDB and PostgreSQL for my next project. Could you " +
            "compare them and help me understand which is better for a social media application? " +
            "I'd appreciate a balanced and honest perspective.",

            "Hey! Could you compare Docker and virtual machines in terms of resource usage, startup " +
            "time, and isolation? I'm trying to explain the difference clearly to my team. Thanks in advance!"
        );

        // ══════════════════════════════════════════════════════════════
        // COMPARE · HIGH
        // ══════════════════════════════════════════════════════════════
        put("COMPARE_HIGH",
            "Hello! I really hope you're having a fantastic day today! I have been going back and " +
            "forth for a very long time trying to make a decision about something technical, and I " +
            "thought maybe you could help me out. I need to compare AWS and Google Cloud Platform for " +
            "hosting a web application, and I honestly have no idea which one to choose. Could you " +
            "please, if you have the time, give me a detailed comparison of the two? I would love to " +
            "understand the pros and cons of each in detail. I know this might be a lot to ask, and " +
            "I truly appreciate you taking the time to read my message. Thank you so much in advance, " +
            "and please let me know if you need any more information!",

            "Hi there! I hope you are doing absolutely wonderfully today! I've been spending a lot of " +
            "time reading about TypeScript and JavaScript but I keep getting more confused the more I " +
            "read. I was hoping you could possibly help me understand which might be better for a " +
            "large-scale enterprise application. If you could compare them in terms of type safety, " +
            "developer experience, and long-term maintainability, I would be so unbelievably grateful. " +
            "I know this is a big ask and I don't want to take up too much of your valuable time. " +
            "Please do let me know if you need any additional context at all. Thanks a million!",

            "Good day! I genuinely hope you are having a most wonderful and productive day! I am " +
            "writing with a somewhat involved question that I hope you will have the patience to " +
            "help me with. I have been tasked with evaluating two state management solutions — " +
            "Redux and Zustand — for our React application, and I find myself quite unable to " +
            "determine which would be the better fit. Could you please, at your earliest convenience " +
            "and if it is not too much trouble, provide a thorough comparison addressing performance, " +
            "ease of use, and community support? I am so very thankful for your time and any " +
            "assistance you can provide. Please don't hesitate to reach out if you need more context. " +
            "Thank you so very much, I truly appreciate it from the bottom of my heart!"
        );
    }

    // ── Private helper ──────────────────────────────────────────────────────────
    private static void put(String key, String... variants) {
        TEMPLATES.put(key, List.of(variants));
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Generate (pick) a prompt from the template library.
     *
     * @param taskType  must be one of: CODING, EXPLAIN, DEBUG, WRITING, COMPARE
     * @param verbosity must be one of: LOW, MEDIUM, HIGH
     * @return a randomly selected template string, or {@code null} if the
     *         combination is not found (caller should validate inputs first)
     */
    public String generate(String taskType, String verbosity) {
        String key = taskType.toUpperCase() + "_" + verbosity.toUpperCase();
        List<String> candidates = TEMPLATES.get(key);
        if (candidates == null || candidates.isEmpty()) return null;
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    /** Returns all valid task type strings. */
    public static final Set<String> VALID_TASK_TYPES =
            Set.of("CODING", "EXPLAIN", "DEBUG", "WRITING", "COMPARE");

    /** Returns all valid verbosity level strings. */
    public static final Set<String> VALID_VERBOSITY_LEVELS =
            Set.of("LOW", "MEDIUM", "HIGH");
}
