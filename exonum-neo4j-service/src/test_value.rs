encoding_struct! {
    ///Our test variable, which we are going to change.
    struct TestValue {
        name:  &str,
        value: u64,
    }
}

impl TestValue {
    /// Returns a copy of this test variable with updated value.
    pub fn set_value(self, new_value: u64) -> Self {
        Self::new(
            self.name(),
            new_value,
        )
    }
}
