package us.example.batch;

import org.springframework.batch.item.file.mapping.DefaultLineMapper;

public class PositionLineMapper extends DefaultLineMapper<Position> {

    @Override
    public Position mapLine(String line, int lineNumber) throws Exception {
        Position t = new Position();

        try {
            t = super.mapLine(line, lineNumber);
        } catch (Exception e) {
            System.out.println(String.format("Unable to parse line number <<{%s}>> with line <<{%s}>>.", lineNumber, line));
        }

        return t;
    }
}
