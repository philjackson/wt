#!/usr/bin/env perl

use strict;
use warnings;

sub tits {
    my @bits = split('\t');
    return '"' . $bits[1] . '":"' . $bits[17] . "\"\n";    
}

print "{\n";

$_ = <>;
print tits();

while(<>) {
    print ", " . tits();
}
print "}\n";
