package org.acme.util;

import org.acme.dto.CustomerDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScreenPrinterTest {

    @Test
    void printsFieldsAtConfiguredPositions() {
        CustomerDTO c = new CustomerDTO();
        c.setFirstname("Jane");
        c.setLastname("Doe");
        c.setAddress("123 Main St");
        c.setCode("X9");

        List<FieldConfig> cfg = Arrays.asList(
                new FieldConfig("firstname", 0),
                new FieldConfig("lastname", 15),
                new FieldConfig("address", 35),
                new FieldConfig("code", 70)
        );

        String line = ScreenPrinter.printLine(c, cfg);
        System.out.println("Parsed line: [" + line + "]");
        assertEquals(80, line.length());
        assertEquals("Jane", line.substring(0, 4));
        assertEquals("Doe", line.substring(15, 18));
        assertEquals("123 Main St", line.substring(35, 46));
        assertEquals("X9", line.substring(70, 72));
    }

    @Test
    void truncatesValuesBeyondLineWidth() {
        CustomerDTO c = new CustomerDTO();
        c.setFirstname("A".repeat(200));

        List<FieldConfig> cfg = Collections.singletonList(
                new FieldConfig("firstname", 0)
        );

        String line = ScreenPrinter.printLine(c, cfg);
        assertEquals(80, line.length());
        // All characters are 'A' because we started at 0 and truncate to 80
        assertEquals("A".repeat(80), line);
    }

    @Test
    void respectsStartIndexAndTruncation() {
        CustomerDTO c = new CustomerDTO();
        c.setFirstname("HELLO-WORLD");

        List<FieldConfig> cfg = Collections.singletonList(
                new FieldConfig("firstname", 78)
        );

        String line = ScreenPrinter.printLine(c, cfg);
        assertEquals(80, line.length());
        // Only first two chars fit at index 78 and 79
        assertEquals('H', line.charAt(78));
        assertEquals('E', line.charAt(79));
    }

    @Test
    void laterFieldsOverwriteEarlierOnOverlap() {
        CustomerDTO c = new CustomerDTO();
        c.setFirstname("AAAA");
        c.setLastname("BBBB");

        List<FieldConfig> cfg = Arrays.asList(
                new FieldConfig("firstname", 10), // positions 10-13
                new FieldConfig("lastname", 12)   // positions 12-15, overwrites 12-13
        );

        String line = ScreenPrinter.printLine(c, cfg);
        assertEquals(80, line.length());
        assertEquals("AA", line.substring(10, 12));
        assertEquals("BB", line.substring(12, 14));
        assertEquals("BB", line.substring(14, 16));
    }

    @Test
    void missingOrNullFieldsRenderAsSpaces() {
        CustomerDTO c = new CustomerDTO();
        c.setFirstname(null); // explicit null
        // lastname not set at all

        List<FieldConfig> cfg = Arrays.asList(
                new FieldConfig("firstname", 0),
                new FieldConfig("lastname", 5)
        );

        String line = ScreenPrinter.printLine(c, cfg);
        assertEquals(80, line.length());
        assertEquals(" ", String.valueOf(line.charAt(0))); // space at index 0
        assertEquals(" ", String.valueOf(line.charAt(5))); // space at index 5
    }

    @Test
    void printsMultipleRecords() {
        CustomerDTO c1 = new CustomerDTO();
        c1.setFirstname("A");
        CustomerDTO c2 = new CustomerDTO();
        c2.setFirstname("B");

        List<FieldConfig> cfg = Collections.singletonList(new FieldConfig("firstname", 0));
        String[] lines = ScreenPrinter.print(Arrays.asList(c1, c2), cfg);
        assertEquals(2, lines.length);
        assertEquals('A', lines[0].charAt(0));
        assertEquals('B', lines[1].charAt(0));
        assertEquals(80, lines[0].length());
        assertEquals(80, lines[1].length());
    }

    @Test
    void printsMultiLineRecord() {
        CustomerDTO c = new CustomerDTO();
        c.setFirstname("Jane");
        c.setLastname("Doe");
        c.setAddress("123 Main St");
        c.setCode("X9");

        // firstname and lastname on line 1, address and code on line 2
        List<FieldConfig> cfg = Arrays.asList(
                new FieldConfig("firstname", 0, 1),   // Line 1
                new FieldConfig("lastname", 15, 1),  // Line 1
                new FieldConfig("address", 0, 2),    // Line 2
                new FieldConfig("code", 50, 2)        // Line 2
        );

        String[] lines = ScreenPrinter.printLines(c, cfg);
        System.out.println("Line 1: [" + lines[0] + "]");
        System.out.println("Line 2: [" + lines[1] + "]");

        assertEquals(2, lines.length);
        // Line 1 should have firstname and lastname
        assertEquals("Jane", lines[0].substring(0, 4));
        assertEquals("Doe", lines[0].substring(15, 18));
        assertEquals(80, lines[0].length());
        // Line 2 should have address and code
        assertEquals("123 Main St", lines[1].substring(0, 11));
        assertEquals("X9", lines[1].substring(50, 52));
        assertEquals(80, lines[1].length());
    }

    @Test
    void skipsEmptyLines() {
        CustomerDTO c = new CustomerDTO();
        c.setFirstname("Jane");
        // lastname is null
        // address is null
        // code is null

        // Line 1 has firstname (populated), Line 2 has address (null), Line 3 has code (null)
        List<FieldConfig> cfg = Arrays.asList(
                new FieldConfig("firstname", 0, 1),   // Line 1 - has value
                new FieldConfig("address", 0, 2),    // Line 2 - null, should be skipped
                new FieldConfig("code", 0, 3)        // Line 3 - null, should be skipped
        );

        String[] lines = ScreenPrinter.printLines(c, cfg);
        System.out.println("Lines returned: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            System.out.println("Line " + (i + 1) + ": [" + lines[i] + "]");
        }

        // Should only return 1 line (line 1), not 3
        assertEquals(1, lines.length);
        assertEquals("Jane", lines[0].substring(0, 4));
        assertEquals(80, lines[0].length());
    }

    @Test
    void printsMultipleRecordsWithEmptyLinesSkipped() {
        // Record 1: has content on both lines
        CustomerDTO c1 = new CustomerDTO();
        c1.setFirstname("Alice");
        c1.setLastname("Smith");
        c1.setAddress("100 Oak St");
        c1.setCode("A1");

        // Record 2: has content on line 1, but line 2 fields are null (line 2 should be skipped)
        CustomerDTO c2 = new CustomerDTO();
        c2.setFirstname("Bob");
        c2.setLastname("Jones");
        // address is null
        // code is null

        // Record 3: has content on both lines
        CustomerDTO c3 = new CustomerDTO();
        c3.setFirstname("Carol");
        c3.setLastname("Brown");
        c3.setAddress("200 Pine Ave");
        c3.setCode("C3");

        // Configure: Line 1 = firstname, lastname; Line 2 = address, code
        List<FieldConfig> cfg = Arrays.asList(
                new FieldConfig("firstname", 0, 1),   // Line 1
                new FieldConfig("lastname", 15, 1),  // Line 1
                new FieldConfig("address", 0, 2),    // Line 2
                new FieldConfig("code", 50, 2)        // Line 2
        );

        String[] lines = ScreenPrinter.print(Arrays.asList(c1, c2, c3), cfg);
        
        System.out.println("Total lines returned: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            System.out.println("Line " + (i + 1) + ": [" + lines[i] + "]");
        }

        // Record 1: 2 lines (line 1 + line 2)
        // Record 2: 1 line (line 1 only, line 2 is empty so skipped)
        // Record 3: 2 lines (line 1 + line 2)
        // Total: 5 lines
        assertEquals(5, lines.length);
        
        // Record 1 - Line 1
        assertEquals("Alice", lines[0].substring(0, 5));
        assertEquals("Smith", lines[0].substring(15, 20));
        // Record 1 - Line 2
        assertTrue(lines[1].substring(0, 10).startsWith("100 Oak St"));
        assertEquals("A1", lines[1].substring(50, 52));
        
        // Record 2 - Line 1 (only line, line 2 was skipped)
        assertEquals("Bob", lines[2].substring(0, 3));
        assertEquals("Jones", lines[2].substring(15, 20));
        
        // Record 3 - Line 1
        assertEquals("Carol", lines[3].substring(0, 5));
        assertEquals("Brown", lines[3].substring(15, 20));
        // Record 3 - Line 2
        assertTrue(lines[4].substring(0, 12).startsWith("200 Pine Ave"));
        assertEquals("C3", lines[4].substring(50, 52));
    }

    @Test
    void indentsLine2() {
        CustomerDTO c = new CustomerDTO();
        c.setFirstname("John");
        c.setLastname("Doe");
        c.setAddress("123 Main St");

        // Line 1: firstname, lastname at normal positions
        // Line 2: address indented 5 spaces
        List<FieldConfig> cfg = Arrays.asList(
                new FieldConfig("firstname", 0, 1),   // Line 1, start at 0
                new FieldConfig("lastname", 15, 1),  // Line 1, start at 15
                new FieldConfig("address", 5, 2)     // Line 2, indented 5 spaces
        );

        String[] lines = ScreenPrinter.printLines(c, cfg);
        System.out.println("Line 1 (not indented): [" + lines[0] + "]");
        System.out.println("Line 2 (indented 5):    [" + lines[1] + "]");

        assertEquals(2, lines.length);
        // Line 1: firstname at 0, lastname at 15
        assertEquals("John", lines[0].substring(0, 4));
        assertEquals("Doe", lines[0].substring(15, 18));
        // Line 2: address indented 5 spaces (starts at index 5)
        assertEquals("123 Main St", lines[1].substring(5, 16));
        // Verify the indentation - first 5 chars should be spaces
        assertEquals("     ", lines[1].substring(0, 5));
    }
}
