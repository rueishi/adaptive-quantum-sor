// Unit tests for the deterministic CUDA tactical optimizer MVP scoring helper.
#include "../cuda_tactical_optimizer.h"

#include <cassert>

int main() {
    assert(sor_cuda_tactical_score(1000, 9000, 0) == 10000);
    assert(sor_cuda_tactical_score(500, 5000, 1000) == 4500);
    assert(sor_cuda_tactical_score(0, 100, 500) == 0);
    assert(sor_cuda_tactical_score(9000, 9000, 0) == 10000);
    return 0;
}
