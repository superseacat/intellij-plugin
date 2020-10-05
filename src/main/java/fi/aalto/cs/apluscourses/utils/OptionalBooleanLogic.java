package fi.aalto.cs.apluscourses.utils;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BinaryOperator;
import org.jetbrains.annotations.NotNull;

public class OptionalBooleanLogic {

  private OptionalBooleanLogic() {

  }

  private static Optional<Boolean> apply(@NotNull BinaryOperator<Boolean> operator,
                                         @NotNull Optional<Boolean>[] operands) {
    return Arrays.stream(operands)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(operator);
  }

  @SafeVarargs
  public static Optional<Boolean> and(Optional<Boolean>... conjuncts) {
    return apply(Boolean::logicalAnd, conjuncts);
  }

  @SafeVarargs
  public static Optional<Boolean> or(Optional<Boolean>... disjuncts) {
    return apply(Boolean::logicalOr, disjuncts);
  }
}
