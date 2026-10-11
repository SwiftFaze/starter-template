package com.swiftfaze.veil.mods;

import org.jspecify.annotations.Nullable;

public class CalcExpressionParser {

    private CalcExpressionParser() {
    }

    public static double evaluate(String expression, int level) {
        Tokenizer tokenizer = new Tokenizer(expression);
        Parser parser = new Parser(tokenizer, level);
        return parser.parseExpression();
    }

    private static class Tokenizer {
        private final String input;
        private int pos = 0;

        Tokenizer(String input) {
            this.input = input.trim();
        }

        @Nullable String nextToken() {
            skipWhitespace();
            if (pos >= input.length()) {
                return null;
            }
            char ch = input.charAt(pos);
            if (isSingleCharOperator(ch)) {
                return consumeOperator();
            }
            if (Character.isLetter(ch)) {
                return consumeIdentifier();
            }
            if (Character.isDigit(ch) || ch == '.') {
                return consumeNumber();
            }
            throw new IllegalArgumentException("Unexpected character: " + ch);
        }

        private static boolean isSingleCharOperator(char ch) {
            return ch == '(' || ch == ')' || ch == '+' || ch == '-' || ch == '*' || ch == '/';
        }

        private String consumeOperator() {
            return String.valueOf(input.charAt(pos++));
        }

        private String consumeIdentifier() {
            int start = pos;
            while (pos < input.length() && Character.isLetterOrDigit(input.charAt(pos))) {
                pos++;
            }
            return input.substring(start, pos);
        }

        private String consumeNumber() {
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            return input.substring(start, pos);
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }

    private static class Parser {
        private final Tokenizer tokenizer;
        private final int level;
        private String currentToken;

        Parser(Tokenizer tokenizer, int level) {
            this.tokenizer = tokenizer;
            this.level = level;
            this.currentToken = tokenizer.nextToken();
        }

        double parseExpression() {
            double result = parseTerm();
            while (currentToken != null && (currentToken.equals("+") || currentToken.equals("-"))) {
                String op = currentToken;
                currentToken = tokenizer.nextToken();
                double right = parseTerm();
                result = op.equals("+") ? result + right : result - right;
            }
            return result;
        }

        private double parseTerm() {
            double result = parseFactor();
            while (currentToken != null && (currentToken.equals("*") || currentToken.equals("/"))) {
                String op = currentToken;
                currentToken = tokenizer.nextToken();
                double right = parseFactor();
                result = op.equals("*") ? result * right : result / right;
            }
            return result;
        }

        private double parseFactor() {
            if (currentToken == null) {
                throw new IllegalArgumentException("Unexpected end of expression");
            }
            return switch (currentToken) {
                case "-" -> parseNegation();
                case "(" -> parseParenthesizedExpression();
                case "level" -> parseLevel();
                default -> parseNumber();
            };
        }

        private double parseNegation() {
            currentToken = tokenizer.nextToken();
            return -parseFactor();
        }

        private double parseParenthesizedExpression() {
            currentToken = tokenizer.nextToken();
            double result = parseExpression();
            if (!")".equals(currentToken)) {
                throw new IllegalArgumentException("Expected ')'");
            }
            currentToken = tokenizer.nextToken();
            return result;
        }

        private double parseLevel() {
            currentToken = tokenizer.nextToken();
            return level;
        }

        private double parseNumber() {
            try {
                double result = Double.parseDouble(currentToken);
                currentToken = tokenizer.nextToken();
                return result;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unexpected token: " + currentToken, e);
            }
        }
    }
}
